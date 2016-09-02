package eventsrc4j;

import java.time.Instant;
import java.util.Optional;
import java.util.function.Function;

/**
 * Read and write actions on a stream.
 *
 * @param <K> events key type.
 * @param <S> sequence used for ordering events in the stream.
 * @param <E> concrete domain events type.
 * @param <R> action result type.
 * @see eventsrc4j.io.WStreamActionIO for an IO interpreter.
 */
@FunctionalInterface
public interface WStreamAction<K, S, E, R> {

  /**
   * Interpreter of base stream actions.
   *
   * @param <R> action result type.
   * @param <X> interpreted action result type (eg. wrapped in a container).
   */
  interface Interpreter<K, S, E, R, X> extends StreamAction.Interpreter<K, S, E, R, X> {
    X Write(Optional<S> expectedSeq, Instant time, Iterable<E> events,
        Function<WriteResult<K, S, E>, R> withResult);
  }

  /**
   * Extends the interpreter to an (operational) monad algebra.
   */
  interface Algebra<K, S, E, R, X>
      extends Interpreter<K, S, E, R, X>, StreamAction.Algebra<K, S, E, R, X> {

    <Q> X Bind(WStreamAction<K, S, E, Q> action, Function<Q, WStreamAction<K, S, E, R>> function);

    @Override
    default <Q> X Bind(StreamAction<K, S, E, Q> action,
        Function<Q, StreamAction<K, S, E, R>> function) {
      return Bind(of(action), function.andThen(WStreamAction::of));
    }

    default <Q> X Map(WStreamAction<K, S, E, Q> action, Function<Q, R> function) {
      return Bind(action, q -> new WStreamAction<K, S, E, R>() {
        @Override public <X2> X2 eval(Algebra<K, S, E, R, X2> interpreter) {
          return interpreter.Pure(function.apply(q));
        }
      });
    }

    @Override
    default <Q> X Map(StreamAction<K, S, E, Q> action, Function<Q, R> function) {
      return Map(of(action), function);
    }
  }

  static <K, S, E, R> WStreamAction<K, S, E, R> of(StreamAction<K, S, E, R> streamAction) {
    return streamAction::eval;
  }

  static <K, S, E, R> WStreamAction<K, S, E, R> Read(Optional<S> fromSeq,
      StreamReader<K, S, E, R> streamReader) {
    return of(StreamAction.Read(fromSeq, streamReader));
  }

  static <K, S, E, R> WStreamAction<K, S, E, R> Latest(
      Function<Optional<Event<K, S, E>>, R> eventReader) {
    return of(StreamAction.Latest(eventReader));
  }

  static <K, S, E, R> WStreamAction<K, S, E, R> Pure(R value) {
    return of(StreamAction.Pure(value));
  }

  static <K, S, E, R> WStreamAction<K, S, E, R> Write(Optional<S> expectedSeq, Instant time, Iterable<E> events,
      Function<WriteResult<K, S, E>, R> withResult) {
    return new WStreamAction<K, S, E, R>() {
      @Override public <X> X eval(Algebra<K, S, E, R, X> interpreter) {
        return interpreter.Write(expectedSeq, time, events, withResult);
      }
    };
  }

  default <Q> WStreamAction<K, S, E, Q> map(Function<R, Q> f) {
    return new WStreamAction<K, S, E, Q>() {
      @Override public <X> X eval(Algebra<K, S, E, Q, X> interpreter) {
        return interpreter.Map(WStreamAction.this, f);
      }
    };
  }

  default <Q> WStreamAction<K, S, E, Q> bind(Function<R, WStreamAction<K, S, E, Q>> f) {
    return new WStreamAction<K, S, E, Q>() {
      @Override public <X> X eval(Algebra<K, S, E, Q, X> interpreter) {
        return interpreter.Bind(WStreamAction.this, f);
      }
    };
  }

  <X> X eval(Algebra<K, S, E, R, X> interpreter);
}
