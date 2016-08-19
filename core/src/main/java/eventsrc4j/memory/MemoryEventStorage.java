package eventsrc4j.memory;

import static eventsrc4j.GlobalSeqs.*;
import static java.util.Optional.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
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

import eventsrc4j.Event;
import eventsrc4j.EventStorage;
import eventsrc4j.Events;
import eventsrc4j.GlobalSeq;
import eventsrc4j.IO;
import eventsrc4j.Sequence;
import eventsrc4j.StreamReader;
import eventsrc4j.WriteResult;
import eventsrc4j.WriteResults;

public final class MemoryEventStorage<K, S, E> implements EventStorage<K, S, E> {

    private final ConcurrentMap<K, ConcurrentNavigableMap<S, Event<K, S, E>>> streams = new ConcurrentHashMap<>();

    private final ConcurrentMap<K, S> seqFollowingLastCommit = new ConcurrentHashMap<>();

    private final ConcurrentNavigableMap<S, Event<K, GlobalSeq<S>, E>> globalStream = new ConcurrentSkipListMap<>();

    private final Sequence<S> sequence;

    public MemoryEventStorage(Sequence<S> sequence) {
        this.sequence = sequence;
    }

    private static <K, V> Optional<V> get(Map<K, V> map, K key) {
        return ofNullable(map.get(key));
    }

    @Override
    public IO<WriteResult> write(K key, Optional<S> expectedSeq, Instant time, Stream<E> events) {
        return () -> {

            ConcurrentNavigableMap<S, Event<K, S, E>> streamMap = streams.computeIfAbsent(key, __ -> new ConcurrentSkipListMap<>(sequence));

            class Persist implements Consumer<E> {

                final List<Event<K, S, E>> toPersistInGlobalStream = new ArrayList<>();

                S seq = expectedSeq.map(sequence::next).orElse(sequence.first());

                @Override
                public void accept(E e) {
                    Event<K, S, E> event = Events.Event(key, seq, time, e);
                    toPersistInGlobalStream.add(event);
                    seq = streamMap.putIfAbsent(seq, event) == null
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
                    // first event persist succeed: we can persist all the rest without checks because "seqFollowingLastCommit" acts as an upper bound and hides all
                    // the new sequences entries until we "commit" by updating the next seq for the key.
                    while (spliterator.tryAdvance(persist)) {
                    }
                    // "commit":
                    seqFollowingLastCommit.put(key, persist.seq);
                    // Persist events in global stream:
                    persist.toPersistInGlobalStream.forEach(
                            new Consumer<Event<K, S, E>>() {
                                S nextGlobalSeq = ofNullable(globalStream.lastEntry()).map(Map.Entry::getKey).map(sequence::next).orElse(sequence.first());

                                @Override
                                public void accept(Event<K, S, E> e) {
                                    while (globalStream.putIfAbsent(nextGlobalSeq, Events.<K, S, E, GlobalSeq<S>>modSeq(s -> seq(nextGlobalSeq, s)).apply(e)) != null) {
                                        nextGlobalSeq = sequence.next(globalStream.lastKey());
                                    }
                                    nextGlobalSeq = sequence.next(nextGlobalSeq);
                                }
                            }
                    );

                    writeResult = WriteResults.Success();
                }
            } else {
                // The stream is empty, persisting nothing is a sucess!
                writeResult = WriteResults.Success();
            }

            return writeResult;
        };
    }

    @Override
    public IO<Optional<Event<K, S, E>>> latest(K key) {
        return () -> get(seqFollowingLastCommit, key).flatMap(nextSeq -> get(streams, key).map(stream -> stream.lowerEntry(nextSeq).getValue()));
    }

    @Override
    public <R> IO<R> read(K key, Optional<S> fromSeq, StreamReader<K, S, E, R> streamReader) {
        return () -> streamReader.apply(
                get(seqFollowingLastCommit, key)
                        .flatMap(nextSeq ->
                                get(streams, key).map(stream ->
                                        fromSeq.map(fromSeqExlusive -> stream.subMap(fromSeqExlusive, false, nextSeq, false))
                                                .orElse(stream.headMap(nextSeq))
                                                .values()
                                                .stream()
                                )
                        ).orElseGet(Stream::empty)
        );
    }

    @Override
    public IO<Optional<Event<K, GlobalSeq<S>, E>>> allLatest() {
        return () -> ofNullable(globalStream.lastEntry()).map(Map.Entry::getValue);
    }

    @Override
    public <R> IO<R> readAll(Optional<S> fromGlobalSeq, StreamReader<K, GlobalSeq<S>, E, R> globalStreamReader) {
        return () -> globalStreamReader.apply(
                fromGlobalSeq.map(fromSeqExlusive -> globalStream.tailMap(fromSeqExlusive, false))
                        .orElse(globalStream)
                        .values()
                        .stream()
        );
    }

    @Override
    public <R> IO<R> readAllKeys(Function<Stream<K>, R> keyStreamReader) {
        return () -> keyStreamReader.apply(streams.keySet().stream());
    }
}
