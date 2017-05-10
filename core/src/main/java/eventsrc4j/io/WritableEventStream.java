package eventsrc4j.io;

import eventsrc4j.WStreamAction;
import eventsrc4j.WriteResult;
import fj.data.Option;
import java.time.Instant;

/**
 * A stream of events, that can be read and written into.
 *
 * @param <K> events key type.
 * @param <S> sequence used for ordering events in the stream.
 * @param <E> concrete domain events type.
 */
public interface WritableEventStream<K, S, E> extends EventStream<K, S, E> {

  /**
   * Save the given events at the end of the stream, if at expected sequence.
   *
   * @param expectedSeq expected last saved sequence of the stream, or empty if the stream is
   * expected to be empty.
   * @param time timestamp of the events to save.
   * @param events a list of events to save in the stream.
   * @return an IO action producing the result of the write; either successful or indicating an
   * optimistic concurrency error (duplicated sequence).
   */
  IO<WriteResult<K, S, E>> write(Option<S> expectedSeq, Instant time, Iterable<E> events);

  default <R> IO<R> evalWAction(WStreamAction<K, S, E, R> action) {
    return action.eval(WStreamIOAlgebra.of(this));
  }


}
