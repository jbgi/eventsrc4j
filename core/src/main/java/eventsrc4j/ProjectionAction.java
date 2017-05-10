package eventsrc4j;

import fj.F;
import fj.data.Option;

import static fj.Function.compose;

/**
 * Stream read and projection snapshot actions on a stream.
 *
 * @param <K> events key type.
 * @param <S> sequence used for ordering events in the stream.
 * @param <E> concrete domain events type.
 * @param <V> concrete view type.
 * @param <R> action result type.
 */
@FunctionalInterface
public interface ProjectionAction<K, S, E, V, R> {

  /**
   * Monadic WStreamAction algebra, that is also a StreamAction algebra
   *
   * @param <R> action result type.
   * @param <X> interpreted action result type (eg. wrapped in a container).
   */
  interface Algebra<K, S, E, V, R, X> extends StreamAction.Algebra<K, S, E, R, X>, SnapshotAction.Algebra<S, V, R, X> {

    <Q> X BindP(ProjectionAction<K, S, E, V, Q> action, F<Q, ProjectionAction<K, S, E, V, R>> function);

    // We derive implementation of monadic operations of the StreamAction and SnapshotAction algebra in term of
    // this ProjectionAction algebra:
    @Override
    default <Q> X Bind(StreamAction<K, S, E, Q> action,
        F<Q, StreamAction<K, S, E, R>> function) {
      return BindP(action::eval, compose(a -> a::eval, function));
    }

    @Override
    default <Q> X Bind(SnapshotAction<S, V, Q> action,
        F<Q, SnapshotAction<S, V, R>> function) {
      return BindP(action::eval, compose(a -> a::eval, function));
    }

  }

  static <K, S, E, V> Factory<K, S, E, V> factory() {
    return new Factory<K, S, E, V>() {};
  }

  interface Factory<K, S, E, V> {

    default StreamAction.Factory<K, S, E> streamActionFactory() {
      return StreamAction.factory();
    }

    default SnapshotAction.Factory<S, V> snapshotActionFactory() {
      return SnapshotAction.factory();
    }

    default ProjectionAction<K, S, E, V, Snapshot<S, V>> GetSnapshot(SequenceQuery<S> sequence) {
      return snapshotActionFactory().GetSnapshot(sequence)::eval;
    }

    default ProjectionAction<K, S, E, V, Snapshot<S, V>> PutSnapshot(Snapshot<S, V> snapshot, SnapshotStoreMode mode) {
      return snapshotActionFactory().PutSnapshot(snapshot, mode)::eval;
    }

    default <R> ProjectionAction<K, S, E, V, R> ReadEventStream(Option<S> fromSeq,
        Fold<Event<K, S, E>, R> streamFold) {
      return streamActionFactory().ReadEventStream(fromSeq, streamFold)::eval;
    }

    default ProjectionAction<K, S, E, V, Option<Event<K, S, E>>> GetLatestEvent() {
      return streamActionFactory().GetLatestEvent()::eval;
    }

    default <R> ProjectionAction<K, S, E, V, R> Pure(R value) {
      return streamActionFactory().Pure(value)::eval;
    }
  }

  default <Q> ProjectionAction<K, S, E, V, Q> bind(F<R, ProjectionAction<K, S, E, V, Q>> f) {
    return new ProjectionAction<K, S, E, V, Q>() {
      @Override public <X> X eval(Algebra<K, S, E, V, Q, X> interpreter) {
        return interpreter.BindP(ProjectionAction.this, f);
      }
    };
  }

  default <Q> ProjectionAction<K, S, E, V, Q> map(F<R, Q> f) {
    return bind(r -> ProjectionAction.<K, S, E, V>factory().Pure(f.f(r)));
  }

  <X> X eval(Algebra<K, S, E, V, R, X> interpreter);
}
