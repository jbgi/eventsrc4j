package eventsrc4j;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * A stream of events, that can be read.
 *
 * @param <K> events key type.
 * @param <S> sequence used for ordering events in the stream.
 * @param <E> concrete domain events type.
 */
public interface EventStream<K, S, E> {

  /**
   * Read a stream of events, in order of sequence, from the underlying data store.
   *
   * @param fromSeq The starting sequence to read events from (exclusive). Empty to read from the
   * start.
   * @param streamReader a fold on the stream of events.
   * @param <R> the results of the stream fold. Must NOT reference the folded {@link Stream}.
   * @return an IO action producing the result of the stream fold. The {@link Stream} is closed and
   * unreadable after execution of the action.
   */
  <R> IO<R> read(Optional<S> fromSeq, StreamReader<K, S, E, R> streamReader);
}
