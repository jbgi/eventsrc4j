package eventsrc4j;

import eventsrc4j.io.StreamIOAlgebra;
import java.util.Optional;
import java.util.function.Function;

/**
 * Read actions on a stream
 *
 * @param <K> events key type.
 * @param <S> sequence used for ordering events in the stream.
 * @param <E> concrete domain events type.
 * @param <R> action result type.
 *
 * @see StreamIOAlgebra for an IO interpreter.
 */
@FunctionalInterface
public interface StreamAction<K, S, E, R> {

  /**
   * Monadic StreamAction algebra.
   * @param <R> action result type.
   * @param <X> interpreted action result type (eg. wrapped in a container).
   */
  interface Algebra<K, S, E, R, X> extends Pure<R, X> {

    X Read(Optional<S> fromSeq, StreamReader<K, S, E, R> streamReader);

    X Latest(Function<Optional<Event<K, S, E>>, R> eventReader);

    <Q> X Bind(StreamAction<K, S, E, Q> action, Function<Q, StreamAction<K, S, E, R>> function);

    default <Q> X Map(StreamAction<K, S, E, Q> action, Function<Q, R> function) {
      return Bind(action, q -> new StreamAction<K, S, E, R>() {
        @Override public <X2> X2 eval(Algebra<K, S, E, R, X2> interpreter) {
          return interpreter.Pure(function.apply(q));
        }
      });
    }
    
  }

  static <K, S, E, R> StreamAction<K, S, E, R> Read(Optional<S> fromSeq,
      StreamReader<K, S, E, R> streamReader) {
    return new StreamAction<K, S, E, R>() {
      @Override public <X> X eval(Algebra<K, S, E, R, X> interpreter) {
        return interpreter.Read(fromSeq, streamReader);
      }
    };
  }

  static <K, S, E, R> StreamAction<K, S, E, R> Latest(
      Function<Optional<Event<K, S, E>>, R> eventReader) {
    return new StreamAction<K, S, E, R>() {
      @Override public <X> X eval(Algebra<K, S, E, R, X> interpreter) {
        return interpreter.Latest(eventReader);
      }
    };
  }

  static <K, S, E, R> StreamAction<K, S, E, R> Pure(R value) {
    return new StreamAction<K, S, E, R>() {
      @Override public <X> X eval(Algebra<K, S, E, R, X> interpreter) {
        return interpreter.Pure(value);
      }
    };
  }

  default <Q> StreamAction<K, S, E, Q> map(Function<R, Q> f) {
    return new StreamAction<K, S, E, Q>() {
      @Override public <X> X eval(Algebra<K, S, E, Q, X> interpreter) {
        return interpreter.Map(StreamAction.this, f);
      }
    };
  }

  default <Q> StreamAction<K, S, E, Q> bind(Function<R, StreamAction<K, S, E, Q>> f) {
    return new StreamAction<K, S, E, Q>() {
      @Override public <X> X eval(Algebra<K, S, E, Q, X> interpreter) {
        return interpreter.Bind(StreamAction.this, f);
      }
    };
  }


  default <V> ProjectionAction<K,S,E,V,R> asProjectionA() {
    return this::eval;
  }

  <X> X eval(Algebra<K, S, E, R, X> interpreter);

}
