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

      ConcurrentNavigableMap<S, Event<K, S, E>> streamMap = streamsByKey.computeIfAbsent(key,
          __ -> new ConcurrentSkipListMap<S, Event<K, S, E>>(sequence));

      class Persist implements Consumer<E> {

        S seq = expectedSeq.map(sequence::next).orElse(sequence.first());

        @Override public void accept(E e) {
          seq = streamMap.putIfAbsent(seq, Events.Event(key, seq, time, e)) == null
                ? sequence.next(seq)
                : null;
        }
      }

      Persist persist = new Persist();

      Spliterator<E> spliterator = events.spliterator();

      synchronized (streamMap) {
        S nextSeq = null;
        while (spliterator.tryAdvance(persist) && (nextSeq = persist.seq) != null) {
        }
        if (nextSeq != null) {
          nextSeqByKey.put(key, nextSeq);
        }
      }

      return persist.seq != null
             ? WriteResults.Success()
             : WriteResults.DuplicateEventSeq();
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
