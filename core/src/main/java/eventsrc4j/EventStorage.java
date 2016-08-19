package eventsrc4j;

import java.time.Instant;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * A source of events.
 * Implementations wrap around an underlying data store (e.g. in-memory map, RDBMS, noSql store).
 *
 * @param <K> event streams keys.
 * @param <S> sequence used for ordering events in a stream.
 * @param <E> concrete domain events.
 */
public interface EventStorage<K, S, E> {

    /**
     * Save the given events.
     *
     * @param key the key of the stream where to save the events.
     * @param expectedSeq expected last saved sequence of the stream, or empty if the stream is expected to be empty.
     * @param time timestamp of the events to save.
     * @param events a list of events to save in the stream.
     * @return an IO action producing the result of the write; either successful or indicating an optimistic concurrency error (duplicated sequence).
     */
    IO<WriteResult> write(K key, Optional<S> expectedSeq, Instant time, Stream<E> events);

    /**
     * Get the latest event of a stream.
     *
     * @param key The key of the stream.
     * @return an IO action producing the single latest event, if found.
     */
    IO<Optional<Event<K, S, E>>> latest(K key);

    /**
     * Read a stream of events, in order of sequence, from the underlying data store.
     *
     * @param key The key
     * @param fromSeq The starting sequence to read events from (exclusive). Empty to read from the start.
     * @param streamReader a fold on the stream of events.
     * @param <R> the results of the stream fold. Must NOT reference the folded {@link Stream}.
     * @return an IO action producing the result of the stream fold. The {@link Stream} is closed and unreadable after execution of the action.
     */
    <R> IO<R> read(K key, Optional<S> fromSeq, StreamReader<K, S, E, R> streamReader);

    /**
     * Get the latest event (by global sequence) from the global stream of all events.
     * This method can be called asynchronously after event writes to ensure that the global sequence is roughly chronological.
     *
     * @return an IO action producing the single latest event, if found.
     */
    IO<Optional<Event<K, GlobalSeq<S>, E>>> allLatest();

    /**
     * Read from all streams of events, in order of global sequence (relative to this event storage).
     * The events are (only) guaranteed to appear in order of sequence relatively to each stream key.
     *
     * @param fromGlobalSeq The starting sequence to read events from (exclusive). None to read from the start.
     * @param globalStreamReader fold on the stream of all events.
     * @param <R> the results of the stream fold. Must NOT reference the folded {@link Stream}.
     * @return an IO action producing the result of the stream fold. The {@link Stream} is closed and unreadable after execution of the action.
     */
    <R> IO<R> readAll(Optional<S> fromGlobalSeq, StreamReader<K, GlobalSeq<S>, E, R> globalStreamReader);

    /**
     * Read from all streams of events, in order of global sequence (relative to this event storage).
     * The events are (only) guaranteed to appear in order of sequence relatively to each stream key.
     *
     * @param keyStreamReader fold on the stream of all keys.
     * @param <R> the results of the stream fold. Must NOT reference the folded {@link Stream}.
     * @return an IO action producing the result of the stream fold. The {@link Stream} is closed and unreadable after execution of the action.
     */
    <R> IO<R> readAllKeys(Function<Stream<K>, R> keyStreamReader);

    default <KK, SS, EE> EventStorage<KK, SS, EE> xmap(Function<K, KK> kk, Function<KK, K> k, Function<S, SS> ss, Function<SS, S> s, Function<E, EE> ee,
            Function<EE, E> e) {

        throw new UnsupportedOperationException("TODO");

    }

}
