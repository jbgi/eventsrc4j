package eventsrc4j.memory;

import eventsrc4j.Event;
import eventsrc4j.EventStorage;
import eventsrc4j.Events;
import eventsrc4j.GlobalSeq;
import eventsrc4j.IO;
import eventsrc4j.Sequence;
import eventsrc4j.StreamReader;
import eventsrc4j.WriteResult;
import eventsrc4j.WriteResults;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Spliterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.Optional.ofNullable;

public final class MemoryEventStorage<K, S, E> implements EventStorage<K, S, E> {

  private final ConcurrentMap<K, ConcurrentNavigableMap<S, Event<K, S, E>>> streamsByKey = new ConcurrentHashMap<>();

  private final ConcurrentMap<K, S> nextSeqByKey = new ConcurrentHashMap<>();

  private final Sequence<S> sequence;

  public MemoryEventStorage(Sequence<S> sequence) {
    this.sequence = sequence;
  }

  private static <K, V> Optional<V> get(Map<K, V> map, K key) {
    return ofNullable(map.get(key));
  }

  @Override public IO<WriteResult> write(K key, Optional<S> expectedSeq, Instant time, Stream<E> events) {
    return () -> {

      ConcurrentNavigableMap<S, Event<K, S, E>> streamMap = streamsByKey.computeIfAbsent(key, __ -> new ConcurrentSkipListMap<>(sequence));

      class Persist implements Consumer<E> {

        S seq = expectedSeq.map(sequence::next).orElse(sequence.first());

        @Override public void accept(E e) {
          seq = streamMap.putIfAbsent(seq, Events.Event(key, seq, time, e)) == null
                // If we could insert into the map we increment the sequence for the next event
                ? sequence.next(seq)
                // otherwise we use null to indicate that another thread was quicker persist an event for the current sequence
                : null;
        }
      }

      Persist persist = new Persist();

      Spliterator<E> spliterator = events.spliterator();

      WriteResult writeResult;
      if (spliterator.tryAdvance(persist)) {
        if (persist.seq == null) {
          // null seq => the sequence already exist in the stream:
          writeResult = WriteResults.DuplicateEventSeq();
        } else {
          // first event persit succeed: we can persist all the rest without checks because "nextSeqByKey" acts as an upper bound and hides all
          // the new sequences entries until we "commit" by updating the next seq for the key.
          while (spliterator.tryAdvance(persist)) {
          }
          // "commit":
          nextSeqByKey.put(key, persist.seq);
          writeResult = WriteResults.Success();
        }
      } else {
        // The stream is empty, persisting nothing is a sucess!
        writeResult = WriteResults.Success();
      }

      return writeResult;
    };
  }

  @Override public IO<Optional<Event<K, S, E>>> latest(K key) {
    return () -> get(nextSeqByKey, key).flatMap(nextSeq -> get(streamsByKey, key).map(stream -> stream.lowerEntry(nextSeq).getValue()));
  }

  @Override public <R> IO<R> read(K key, Optional<S> fromSeq, StreamReader<K, S, E, R> streamReader) {
    return () -> streamReader.apply(get(nextSeqByKey, key).flatMap(nextSeq -> get(streamsByKey, key).map(
        stream -> fromSeq.map(fromSeqExlusive -> stream.subMap(fromSeqExlusive, false, nextSeq, false))
            .orElse(stream.headMap(nextSeq))
            .values()
            .stream())).orElseGet(Stream::empty));
  }

  @Override public IO<Optional<Event<K, GlobalSeq<S>, E>>> allLatest() {
    throw new UnsupportedOperationException("TODO");
  }

  @Override public <R> IO<R> readAll(Optional<S> fromGlobalSeq, StreamReader<K, GlobalSeq<S>, E, R> globalStreamReader) {
    throw new UnsupportedOperationException("TODO");
  }

  @Override public <R> IO<R> readAllKeys(Function<Stream<K>, R> keyStreamReader) {
    throw new UnsupportedOperationException("TODO");
  }
}
