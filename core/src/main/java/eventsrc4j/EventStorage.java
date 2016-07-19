package eventsrc4j;


import java.util.List;
import java.util.Optional;


public interface EventStorage<K, S, E> {

    /**
     * Retrieve a stream of events from the underlying data store. This stream should take care of pagination and
     * cleanup of any underlying resources (e.g. closing connections if required). Must be in order of sequence.
     * @param key The key
     * @param fromSeq The starting sequence to read events from (exclusive). None to read from the start.
     * @param streamReader fold on the stream of events.
     * @return Stream of events.
     */
    <R> IO<R> read(K key, Optional<S> fromSeq, StreamReader<K, S, E, R> streamReader);

    <R> IO<R> readAll(Optional<S> fromSeq, StreamReader<K, GlobalSeq<S>, E, R> streamReader);

    /**
     * Save the given events.
     *
     * @return Either an Error or the event that was saved. Other non-specific errors should be available
     *         through the container F.
     */
    IO<WriteResult> write(K key, List<Event<?, S, E>> events);

    /**
     * Get the latest event.
     *
     * @param key The key
     * @return Single event if found.
     */
    IO<Optional<Event<K, S, E>>> latest(K key);
    
}
