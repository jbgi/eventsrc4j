package eventsrc4j;


import java.util.List;
import java.util.Optional;

public interface EventStorage<K, S, E> {

    /**
     * Retrieve a stream of events from the underlying data store. This stream should take care of pagination and
     * cleanup of any underlying resources (e.g. closing connections if required). Must be in order of sequence.
     * @param key The key
     * @param fromSeq The starting sequence to get events from (exclusive). None to get from the start.
     * @param eventStreamConsumer fold on the stream of events.
     * @return Stream of events.
     */
    <R> IO<R> get(K key, Optional<S> fromSeq, EventStreamConsumer<S, E, R> eventStreamConsumer);

    /**
     * Save the given events.
     *
     * @return Either an Error or the event that was saved. Other non-specific errors should be available
     *         through the container F.
     */
    IO<PutResult> put(List<Event<S, E>> events);

}
