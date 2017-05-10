package eventsrc4j;

import fj.F;
import fj.data.Option;
import org.derive4j.hkt.TypeEq;

/**
 * ReadEventStream actions on a stream
 *
 * @param <K> events key type.
 * @param <S> sequence used for ordering events in the stream.
 * @param <E> concrete domain events type.
 * @param <R> action result type.
 * @see eventsrc4j.io.StreamIOAlgebra for an IO interpreter.
 */
@FunctionalInterface
public interface StreamAction<K, S, E, R> {

  interface Factory<K, S, E> {

    default <R> StreamAction<K, S, E, R> ReadEventStream(Option<S> fromSeq,
        Fold<Event<K, S, E>, R> streamFold) {
      return new StreamAction<K, S, E, R>() {
        @Override public <X> X eval(Algebra<K, S, E, R, X> interpreter) {
          return interpreter.Read(fromSeq, streamFold);
        }
      };
    }

    default StreamAction<K, S, E, Option<Event<K, S, E>>> GetLatestEvent() {
      return new StreamAction<K, S, E, Option<Event<K, S, E>>>() {
        @Override public <X> X eval(Algebra<K, S, E, Option<Event<K, S, E>>, X> interpreter) {
          return interpreter.Latest(TypeEq.refl());
        }
      };
    }

    default <R> StreamAction<K, S, E, R> Pure(R value) {
      return new StreamAction<K, S, E, R>() {
        @Override public <X> X eval(Algebra<K, S, E, R, X> interpreter) {
          return interpreter.Pure(value);
        }
      };
    }
  }

  /**
   * Monadic StreamAction algebra.
   *
   * @param <R> action result type.
   * @param <X> interpreted action result type (eg. wrapped in a container).
   */
  interface Algebra<K, S, E, R, X> extends Pure<R, X> {

    X Read(Option<S> fromSeq, Fold<Event<K, S, E>, R> streamFold);

    X Latest(TypeEq<Option<Event<K, S, E>>, R> resultType);

    <Q> X Bind(StreamAction<K, S, E, Q> action, F<Q, StreamAction<K, S, E, R>> function);
  }

  static <K, S, E> Factory<K, S, E> factory() {
    return new Factory<K, S, E>() {
    };
  }

  default <Q> StreamAction<K, S, E, Q> map(F<R, Q> f) {
    return new StreamAction<K, S, E, Q>() {
      @Override public <X> X eval(Algebra<K, S, E, Q, X> interpreter) {
        return interpreter.Bind(StreamAction.this, r -> StreamAction.<K, S, E>factory().Pure(f.f(r)));
      }
    };
  }

  default <Q> StreamAction<K, S, E, Q> bind(F<R, StreamAction<K, S, E, Q>> f) {
    return new StreamAction<K, S, E, Q>() {
      @Override public <X> X eval(Algebra<K, S, E, Q, X> interpreter) {
        return interpreter.Bind(StreamAction.this, f);
      }
    };
  }

  <X> X eval(Algebra<K, S, E, R, X> interpreter);
}
