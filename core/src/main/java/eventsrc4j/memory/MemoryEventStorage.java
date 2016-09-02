package eventsrc4j.memory;

import eventsrc4j.Event;
import eventsrc4j.io.EventStorage;
import eventsrc4j.io.EventStream;
import eventsrc4j.Events;
import eventsrc4j.Sequence;
import eventsrc4j.StreamReader;
import eventsrc4j.io.WritableEventStream;
import eventsrc4j.WriteResult;
import eventsrc4j.WriteResults;
import eventsrc4j.io.IO;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static eventsrc4j.WriteResults.Success;
import static eventsrc4j.memory.GlobalSeqs.seq;
import static eventsrc4j.util.Streams.dropWhile;
import static eventsrc4j.util.Streams.takeWhile;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static java.util.Optional.ofNullable;

public final class MemoryEventStorage<K, S, E> implements EventStorage<K, S, E>, EventStream<K, GlobalSeq<S>, E> {

  private final Sequence<S> sequence;

  private final ConcurrentMap<K, ConcurrentNavigableMap<S, Event<K, S, E>>> streams =
      new ConcurrentHashMap<>();

  private final ConcurrentMap<K, S> seqFollowingLastCommit = new ConcurrentHashMap<>();

  private final AtomicReference<S> nextGlobalSeq;

  private final ConcurrentSkipListMap<S, List<Event<K, S, E>>> globalStream =
      new ConcurrentSkipListMap<>();

  public MemoryEventStorage(Sequence<S> sequence) {
    this.sequence = sequence;
    nextGlobalSeq = new AtomicReference<>(sequence.first());
  }

  private static <K, V> Optional<V> get(Map<K, V> map, K key) {
    return ofNullable(map.get(key));
  }

  private static <K, S, E> Stream<Event<K, GlobalSeq<S>, E>> flattenGlobalStream(
      ConcurrentNavigableMap<S, List<Event<K, S, E>>> eventMap) {
    return eventMap
        .entrySet()
        .stream()
        .flatMap(entry -> entry.getValue()
            .stream()
            .map(Events.modSeq(s -> seq(entry.getKey(), s))));
  }

  @Override
  public WritableEventStream<K, S, E> stream(K key) {
    return new WritableEventStream<K, S, E>() {

      @Override public IO<WriteResult<K, S, E>> write(Optional<S> expectedSeq, Instant time,
          Iterable<E> events) {
        return MemoryEventStorage.this.write(key, expectedSeq, time, events);
      }

      @Override public <R> IO<R> read(Optional<S> fromSeq, StreamReader<K, S, E, R> streamReader) {
        return MemoryEventStorage.this.read(key, fromSeq, streamReader);
      }

      @Override public IO<Optional<Event<K, S, E>>> latest() {
        return MemoryEventStorage.this.latest(key);
      }
    };
  }

  @Override
  public <R> IO<R> read(Optional<GlobalSeq<S>> fromSeq,
      StreamReader<K, GlobalSeq<S>, E, R> streamReader) {

    return () -> streamReader.apply(
        takeWhile(
            // We limit the stream to consecutive sequence otherwise we could miss some not yet inserted events
            // (because global sequence number are pre-allocated)
            new Predicate<Event<K, GlobalSeq<S>, E>>() {

              S expectedGlobalSeq = fromSeq.map(GlobalSeqs::getGlobalSeq).orElse(sequence.first());

              @Override public boolean test(Event<K, GlobalSeq<S>, E> e) {
                if (e.seq().globalSeq().equals(expectedGlobalSeq)) {
                  expectedGlobalSeq = sequence.next(expectedGlobalSeq);
                  return true;
                }
                return false;
              }
            }
            ,
            fromSeq.map(fromSeqExlusive ->
                dropWhile(
                    (Event<K, GlobalSeq<S>, E> e) ->
                        e.seq().globalSeq().equals(fromSeqExlusive.globalSeq())
                            && sequence.compare(e.seq().seq(), fromSeqExlusive.seq()) <= 0,

                    flattenGlobalStream(globalStream.tailMap(fromSeqExlusive.globalSeq()))
                )
            ).orElse(flattenGlobalStream(globalStream))
        )
    );
  }

  @Override
  public IO<Optional<Event<K, GlobalSeq<S>, E>>> latest() {
    return () -> ofNullable(globalStream.lastEntry()).map(
        e -> Events.<K, S, E, GlobalSeq<S>>modSeq(s -> seq(e.getKey(), s))
            .apply(e.getValue().get(e.getValue().size() - 1)));
  }

  private IO<WriteResult<K, S, E>> write(K key, Optional<S> expectedSeq, Instant time,
      Iterable<E> events) {
    return () -> {

      ConcurrentNavigableMap<S, Event<K, S, E>> streamMap =
          streams.computeIfAbsent(key, __ -> new ConcurrentSkipListMap<>(sequence));

      ArrayList<Event<K, S, E>> persistedEvents = new ArrayList<>();

      S seq = expectedSeq.map(sequence::next).orElse(sequence.first());

      Iterator<E> iterator = events.iterator();
      while (iterator.hasNext()) {

        Event<K, S, E> event = Events.Event(key, seq, time, iterator.next());
        if (streamMap.putIfAbsent(seq, event) == null) {
          persistedEvents.add(event);
          seq = sequence.next(seq);
        } else {
          // null seq => the sequence already exist in the stream:
          return WriteResults.DuplicateEventSeq();
        }
      }

      return Success(commitSuccessfulWrite(key, persistedEvents, seq));
    };
  }

  private List<Event<K, S, E>> commitSuccessfulWrite(K key,
      ArrayList<Event<K, S, E>> persistedEvents, S seq) {
    if (!persistedEvents.isEmpty()) {
      persistedEvents.trimToSize();
      globalStream.put(nextGlobalSeq(), persistedEvents);

      S nextSeq = seq;
      // "commit":
      seqFollowingLastCommit.compute(key,
          (k, s) -> s == null || sequence.compare(s, nextSeq) < 0
              ? nextSeq
              : s);

      return unmodifiableList(persistedEvents);
    }
    return emptyList();
  }

  private <R> IO<R> read(K key, Optional<S> fromSeq, StreamReader<K, S, E, R> streamReader) {
    return () -> streamReader.apply(
        get(seqFollowingLastCommit, key)
            .flatMap(nextSeq ->
                get(streams, key).map(stream ->
                    fromSeq.map(
                        fromSeqExlusive -> stream.subMap(fromSeqExlusive, false, nextSeq, false))
                        .orElse(stream.headMap(nextSeq))
                        .values()
                        .stream()
                )
            ).orElseGet(Stream::empty)
    );
  }

  private IO<Optional<Event<K, S, E>>> latest(K key) {
    return () -> get(seqFollowingLastCommit, key)
        .flatMap(nextSeq -> get(streams, key)
            .map(stream -> stream.lowerEntry(nextSeq).getValue()));
  }

  private S nextGlobalSeq() {
    return nextGlobalSeq.getAndUpdate(sequence::next);
  }
}
