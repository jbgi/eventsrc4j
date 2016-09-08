package eventsrc4j;

import eventsrc4j.io.WStreamIOAlgebra;
import java.util.function.Function;

/**
 * Read and write and snapshot actions on a stream.
 *
 * @param <K> events key type.
 * @param <S> sequence used for ordering events in the stream.
 * @param <E> concrete domain events type.
 * @param <V> concrete view type.
 * @param <R> action result type.
 * @see WStreamIOAlgebra for an IO interpreter.
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

    <Q> X Bind(ESAction<K, S, E, V, Q> action, Function<Q, ESAction<K, S, E, V, R>> function);

    // derived map implementation from bind/pure:
    default <Q> X Map(ESAction<K, S, E, V, Q> action, Function<Q, R> function) {
      return Bind(action, q -> new ESAction<K, S, E, V, R>() {
        @Override public <X2> X2 eval(Algebra<K, S, E, V, R, X2> interpreter) {
          return interpreter.Pure(function.apply(q));
        }
      });
    }

    // We derive monadic implementation of other algebra of this ESAction algebra:
    @Override
    default <Q> X Bind(WStreamAction<K, S, E, Q> action,
        Function<Q, WStreamAction<K, S, E, R>> function) {
      return Bind(of(action), function.andThen(ESAction::of));
    }

    @Override
    default <Q> X Map(WStreamAction<K, S, E, Q> action, Function<Q, R> function) {
      return Map(of(action), function);
    }

    @Override default <Q> X Bind(ProjectionAction<K, S, E, V, Q> action,
        Function<Q, ProjectionAction<K, S, E, V, R>> function) {
      return Bind(of(action), function.andThen(ESAction::of));
    }

    @Override default <Q> X Map(ProjectionAction<K, S, E, V, Q> action, Function<Q, R> function) {
      return Map(of(action), function);
    }

    @Override default <Q> X Bind(StreamAction<K, S, E, Q> action, Function<Q, StreamAction<K, S, E, R>> function) {
      return Bind(of(action), function.andThen(ESAction::of));
    }

    @Override default <Q> X Map(StreamAction<K, S, E, Q> action, Function<Q, R> function) {
      return Map(of(action), function);
    }

    @Override default <Q> X Bind(SnapshotAction<S, V, Q> action, Function<Q, SnapshotAction<S, V, R>> function) {
      return Bind(of(action), function.andThen(ESAction::of));
    }

    @Override default <Q> X Map(SnapshotAction<S, V, Q> action, Function<Q, R> function) {
      return Map(of(action), function);
    }
  }

  static <K, S, E, V, R> ESAction<K, S, E, V, R> of(WStreamAction<K, S, E, R> streamAction) {
    return streamAction::eval;
  }

  static <K, S, E, V, R> ESAction<K, S, E, V, R> of(StreamAction<K, S, E, R> streamAction) {
    return streamAction::eval;
  }

  static <K, S, E, V, R> ESAction<K, S, E, V, R> of(SnapshotAction<S, V, R> snapshotAction) {
    return snapshotAction::eval;
  }

  static <K, S, E, V, R> ESAction<K, S, E, V, R> of(ProjectionAction<K, S, E, V, R> projectionAction) {
    return projectionAction::eval;
  }

  <X> X eval(Algebra<K, S, E, V, R, X> interpreter);
}
