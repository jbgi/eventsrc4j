package eventsrc4j;

import eventsrc4j.io.ESActionIOAlgebra;
import fj.F;
import fj.data.Option;
import java.time.Instant;
import org.derive4j.hkt.TypeEq;

import static fj.Function.compose;

/**
 * ReadEventStream and write and snapshot actions on a stream.
 *
 * @param <K> events key type.
 * @param <S> sequence used for ordering events in the stream.
 * @param <E> concrete domain events type.
 * @param <V> concrete view type.
 * @param <R> action result type.
 * @see ESActionIOAlgebra for an IO interpreter.
 */
@FunctionalInterface
public interface ESAction<K, S, E, V, R> {

  /**
   * Monadic WStreamAction algebra, that is also a StreamAction algebra
   *
   * @param <R> action result type.
   * @param <X> interpreted action result type (eg. wrapped in a container).
   */
  interface Algebra<K, S, E, V, R, X>
      extends ProjectionAction.Algebra<K, S, E, V, R, X>, WStreamAction.Algebra<K, S, E, R, X> {

    <Q> X BindES(ESAction<K, S, E, V, Q> action, F<Q, ESAction<K, S, E, V, R>> function);

    // We derive monadic implementation of other inherited algebras:
    @Override default <Q> X Bind(StreamAction<K, S, E, Q> action, F<Q, StreamAction<K, S, E, R>> function) {
      return BindES(action::eval, compose(a -> a::eval, function));
    }

    @Override
    default <Q> X BindW(WStreamAction<K, S, E, Q> action,
        F<Q, WStreamAction<K, S, E, R>> function) {
      return BindES(action::eval, compose(a -> a::eval, function));
    }

    @Override default <Q> X BindP(ProjectionAction<K, S, E, V, Q> action,
        F<Q, ProjectionAction<K, S, E, V, R>> function) {
      return BindES(action::eval, compose(a -> a::eval, function));
    }
  }


  static <K, S, E, V> Factory<K, S, E, V> factory() {
    return new Factory<K, S, E, V>() {};
  }

  interface Factory<K, S, E, V> {

    default <R> ESAction<K, S, E, V, R> ESAction(ProjectionAction<K, S, E, V, R> action) {
      return action::eval;
    }

    default WStreamAction.Factory<K, S, E> wstreamActionFactory() {
      return WStreamAction.factory();
    }

    default ESAction<K, S, E, V, WriteResult<K, S, E>> WriteEvents(Option<S> expectedSeq, Instant time,
        Iterable<E> events) {
      return wstreamActionFactory().WriteEvents(expectedSeq, time, events)::eval;
    }

    default ProjectionAction.Factory<K, S, E, V> projectionActionFactory() {
      return ProjectionAction.factory();
    }

    default ESAction<K, S, E, V, Snapshot<S, V>> GetSnapshot(SequenceQuery<S> sequence) {
      return projectionActionFactory().GetSnapshot(sequence)::eval;
    }

    default ESAction<K, S, E, V, Snapshot<S, V>> PutSnapshot(Snapshot<S, V> snapshot, SnapshotStoreMode mode) {
      return projectionActionFactory().PutSnapshot(snapshot, mode)::eval;
    }

    default <R> ESAction<K, S, E, V, R> ReadEventStream(Option<S> fromSeq,
        Fold<Event<K, S, E>, R> streamFold) {
      return projectionActionFactory().ReadEventStream(fromSeq, streamFold)::eval;
    }

    default ESAction<K, S, E, V, Option<Event<K, S, E>>> GetLatestEvent() {
      return projectionActionFactory().GetLatestEvent()::eval;
    }

    default <R> ESAction<K, S, E, V, R> Pure(R value) {
      return projectionActionFactory().Pure(value)::eval;
    }

  }

  interface DelegatingAlgebra<K, S, E, V, R, X> extends Algebra<K, S, E, V, R, X> {

    SnapshotAction.Algebra<S, V, R, X> snapshotAlgebra();

    WStreamAction.Algebra<K, S, E, R, X> wStreamAlgebra();

    @Override default X Get(SequenceQuery<S> sequence, TypeEq<Snapshot<S, V>, R> resultType) {
      return snapshotAlgebra().Get(sequence, resultType);
    }

    @Override default X Put(Snapshot<S, V> snapshot, SnapshotStoreMode mode, TypeEq<Snapshot<S, V>, R> resultType) {
      return snapshotAlgebra().Put(snapshot, mode, resultType);
    }

    @Override default X Write(Option<S> expectedSeq, Instant time, Iterable<E> events, TypeEq<WriteResult<K, S, E>, R> resultType) {
      return wStreamAlgebra().Write(expectedSeq, time, events, resultType);
    }

    @Override default X Read(Option<S> fromSeq, Fold<Event<K, S, E>, R> streamFold) {
      return wStreamAlgebra().Read(fromSeq, streamFold);
    }

    @Override default X Latest(TypeEq<Option<Event<K, S, E>>, R> resultType) {
      return wStreamAlgebra().Latest(resultType);
    }
  }

  default <Q> ESAction<K, S, E, V, Q> bind(F<R, ESAction<K, S, E, V, Q>> f) {
    return new ESAction<K, S, E, V, Q>() {
      @Override public <X> X eval(Algebra<K, S, E, V, Q, X> interpreter) {
        return interpreter.BindES(ESAction.this, f);
      }
    };
  }

  default <Q> ESAction<K, S, E, V, Q> map(F<R, Q> f) {
    return bind(compose(q -> ESAction.<K, S, E, V>factory().Pure(q)::eval, f));
  }

  <X> X eval(Algebra<K, S, E, V, R, X> interpreter);
}
