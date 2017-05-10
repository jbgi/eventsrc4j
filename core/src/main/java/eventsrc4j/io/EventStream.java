package eventsrc4j.io;

import eventsrc4j.Event;
import eventsrc4j.Fold;
import eventsrc4j.StreamAction;
import fj.data.Option;
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
   * ReadEventStream the stream of events, in order of sequence, from the underlying data store.
   *
   * @param <R> the results of the stream fold. Must NOT reference the folded {@link Stream}.
   * @param fromSeq The starting sequence to read events from (exclusive). Empty to read from the
   * start.
   * @param streamFold a fold on the stream of events.
   * @return an IO action producing the result of the stream fold. The {@link Stream} is closed and
   * unreadable after execution of the action.
   */
  <R> IO<R> read(Option<S> fromSeq, Fold<Event<K, S, E>, R> streamFold);

  /**
   * GetSnapshot the latest event of the stream.
   *
   * @return an IO action producing the single latest event, if found.
   */
  IO<Option<Event<K, S, E>>> latest();

  default <R> IO<R> evalAction(StreamAction<K, S, E, R> action) {
    return action.eval(StreamIOAlgebra.of(this));
  }

}
