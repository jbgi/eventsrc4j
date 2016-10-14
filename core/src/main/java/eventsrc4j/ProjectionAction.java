package eventsrc4j;

import java.util.function.Function;

/**
 * Stream read and projection snapshot actions on a stream.
 *
 * @param <K> events key type.
 * @param <S> sequence used for ordering events in the stream.
 * @param <E> concrete domain events type.
 * @param <V> concrete view type.
 * @param <R> action result type.
 * @see ProjectionIOAlgebra for an IO interpreter.
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

    <Q> X Bind(ProjectionAction<K, S, E, V, Q> action, Function<Q, ProjectionAction<K, S, E, V, R>> function);

    // derived map implementation from bind/pure:
    default <Q> X Map(ProjectionAction<K, S, E, V, Q> action, Function<Q, R> function) {
      return Bind(action, q -> new ProjectionAction<K, S, E, V, R>() {
        @Override public <X2> X2 eval(Algebra<K, S, E, V, R, X2> interpreter) {
          return interpreter.Pure(function.apply(q));
        }
      });
    }

    // We derive implementation of monadic operations of the StreamAction and SnapshotAction algebra in term of this ProjectionAction algebra:
    @Override
    default <Q> X Bind(StreamAction<K, S, E, Q> action,
        Function<Q, StreamAction<K, S, E, R>> function) {
      return Bind(ProjectionAction.of(action), function.andThen(ProjectionAction::of));
    }

    @Override
    default <Q> X Map(StreamAction<K, S, E, Q> action, Function<Q, R> function) {
      return Map(ProjectionAction.of(action), function);
    }

    @Override
    default <Q> X Bind(SnapshotAction<S, V, Q> action,
        Function<Q, SnapshotAction<S, V, R>> function) {
      return Bind(ProjectionAction.of(action), function.andThen(ProjectionAction::of));
    }

    @Override
    default <Q> X Map(SnapshotAction<S, V, Q> action, Function<Q, R> function) {
      return Map(ProjectionAction.of(action), function);
    }
  }

  static <K, S, E, V, R> ProjectionAction<K, S, E, V, R> of(StreamAction<K, S, E, R> streamAction) {
    return streamAction::eval;
  }

  static <K, S, E, V, R> ProjectionAction<K, S, E, V, R> of(SnapshotAction<S, V, R> snapshotAction) {
    return snapshotAction::eval;
  }

  static <K, S, E, V, R> ProjectionAction<K, S, E, V, R> Pure(R value) {
    return new ProjectionAction<K, S, E, V, R>() {
      @Override public <X> X eval(Algebra<K, S, E, V, R, X> interpreter) {
        return interpreter.Pure(value);
      }
    };
  }

  default <Q> ProjectionAction<K, S, E, V, Q> map(Function<R, Q> f) {
    return new ProjectionAction<K, S, E, V, Q>() {
      @Override public <X> X eval(Algebra<K, S, E, V, Q, X> interpreter) {
        return interpreter.Map(ProjectionAction.this, f);
      }
    };
  }

  default <Q> ProjectionAction<K, S, E, V, Q> bind(Function<R, ProjectionAction<K, S, E, V, Q>> f) {
    return new ProjectionAction<K, S, E, V, Q>() {
      @Override public <X> X eval(Algebra<K, S, E, V, Q, X> interpreter) {
        return interpreter.Bind(ProjectionAction.this, f);
      }
    };
  }

  default ESAction<K, S, E, V, R> asESAction() {
    return this::eval;
  }

  <X> X eval(Algebra<K, S, E, V, R, X> interpreter);
}
