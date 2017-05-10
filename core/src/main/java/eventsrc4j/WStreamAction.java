package eventsrc4j;

import fj.F;
import fj.data.Option;
import java.time.Instant;
import org.derive4j.hkt.TypeEq;

import static fj.Function.compose;

/**
 * ReadEventStream and write actions on a stream.
 *
 * @param <K> events key type.
 * @param <S> sequence used for ordering events in the stream.
 * @param <E> concrete domain events type.
 * @param <R> action result type.
 * @see eventsrc4j.io.WStreamIOAlgebra for an IO interpreter.
 */
@FunctionalInterface
public interface WStreamAction<K, S, E, R> {

  /**
   * Monadic WStreamAction algebra, that is also a StreamAction algebra
   *
   * @param <R> action result type.
   * @param <X> interpreted action result type (eg. wrapped in a container).
   */
  interface Algebra<K, S, E, R, X> extends StreamAction.Algebra<K, S, E, R, X> {

    X Write(Option<S> expectedSeq, Instant time, Iterable<E> events, TypeEq<WriteResult<K, S, E>, R> resultType);

    <Q> X BindW(WStreamAction<K, S, E, Q> action, F<Q, WStreamAction<K, S, E, R>> function);

    // We derive implementation of monadic operations of the StreamAction algebra in term of this WStreamAction algebra:
    @Override
    default <Q> X Bind(StreamAction<K, S, E, Q> action,
        F<Q, StreamAction<K, S, E, R>> function) {
      return BindW(action::eval, compose(a -> a::eval, function));
    }
  }

  interface Factory<K, S, E> {

    default WStreamAction<K, S, E, WriteResult<K, S, E>> WriteEvents(Option<S> expectedSeq, Instant time,
        Iterable<E> events) {
      return new WStreamAction<K, S, E, WriteResult<K, S, E>>() {
        @Override public <X> X eval(Algebra<K, S, E, WriteResult<K, S, E>, X> interpreter) {
          return interpreter.Write(expectedSeq, time, events, TypeEq.refl());
        }
      };
    }


    default StreamAction.Factory<K, S, E> streamActionFactory() {
      return StreamAction.factory();
    }

    default <R> WStreamAction<K, S, E, R> ReadEventStream(Option<S> fromSeq,
        Fold<Event<K, S, E>, R> streamFold) {
      return streamActionFactory().ReadEventStream(fromSeq, streamFold)::eval;
    }

    default WStreamAction<K, S, E, Option<Event<K, S, E>>> GetLatestEvent() {
      return streamActionFactory().GetLatestEvent()::eval;
    }

    default <R> WStreamAction<K, S, E, R> Pure(R value) {
      return streamActionFactory().Pure(value)::eval;
    }
  }

  static <K, S, E> Factory<K, S, E> factory() {
    return new Factory<K, S, E>() {
    };
  }

  <X> X eval(Algebra<K, S, E, R, X> interpreter);

  default <Q> WStreamAction<K, S, E, Q> map(F<R, Q> f) {
    return bind(r -> WStreamAction.<K, S, E>factory().Pure(f.f(r))::eval);
  }

  default <Q> WStreamAction<K, S, E, Q> bind(F<R, WStreamAction<K, S, E, Q>> f) {
    return new WStreamAction<K, S, E, Q>() {
      @Override public <X> X eval(Algebra<K, S, E, Q, X> interpreter) {
        return interpreter.BindW(WStreamAction.this, f);
      }
    };
  }
}
