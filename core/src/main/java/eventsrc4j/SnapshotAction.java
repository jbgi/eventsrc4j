package eventsrc4j;

import eventsrc4j.io.SnapshotIOAlgebra;
import java.util.function.Function;

/**
 * Actions on a snapshot
 *
 * @param <S> sequence used for ordering events in the stream.
 * @param <V> type of the view stored by the snapshot.
 * @param <R> action result type.
 * @see SnapshotIOAlgebra for an IO interpreter.
 */
@FunctionalInterface
public interface SnapshotAction<S, V, R> {

  /**
   * Monadic SnapshotAction algebra.
   *
   * @param <R> action result type.
   * @param <X> interpreted action result type (eg. wrapped in a container).
   */
  interface Algebra<S, V, R, X> extends Pure<R, X> {

    X Get(SequenceQuery<S> sequence, Function<Snapshot<S, V>, R> snapshotReader);

    X Put(Snapshot<S, V> snapshot, SnapshotStoreMode mode, Function<Snapshot<S, V>, R> id);

    <Q> X Bind(SnapshotAction<S, V, Q> action, Function<Q, SnapshotAction<S, V, R>> function);

    default <Q> X Map(SnapshotAction<S, V, Q> action, Function<Q, R> function) {
      return Bind(action, q -> new SnapshotAction<S, V, R>() {
        @Override public <X2> X2 eval(Algebra<S, V, R, X2> interpreter) {
          return interpreter.Pure(function.apply(q));
        }
      });
    }
  }

  static <S, V, R> SnapshotAction<S, V, R> Get(SequenceQuery<S> sequence, Function<Snapshot<S, V>, R> snapshotReader) {
    return new SnapshotAction<S, V, R>() {
      @Override public <X> X eval(Algebra<S, V, R, X> interpreter) {
        return interpreter.Get(sequence, snapshotReader);
      }
    };
  }

  static <S, V> SnapshotAction<S, V, Snapshot<S, V>> Put(Snapshot<S, V> snapshot, SnapshotStoreMode mode) {
    return new SnapshotAction<S, V, Snapshot<S, V>>() {
      @Override public <X> X eval(Algebra<S, V, Snapshot<S, V>, X> interpreter) {
        return interpreter.Put(snapshot, mode, s -> s);
      }
    };
  }

  static <S, V, R> SnapshotAction<S, V, R> Pure(R value) {
    return new SnapshotAction<S, V, R>() {
      @Override public <X> X eval(Algebra<S, V, R, X> interpreter) {
        return interpreter.Pure(value);
      }
    };
  }

  default <Q> SnapshotAction<S, V, Q> map(Function<R, Q> f) {
    return new SnapshotAction<S, V, Q>() {
      @Override public <X> X eval(Algebra<S, V, Q, X> interpreter) {
        return interpreter.Map(SnapshotAction.this, f);
      }
    };
  }

  default <Q> SnapshotAction<S, V, Q> bind(Function<R, SnapshotAction<S, V, Q>> f) {
    return new SnapshotAction<S, V, Q>() {
      @Override public <X> X eval(Algebra<S, V, Q, X> interpreter) {
        return interpreter.Bind(SnapshotAction.this, f);
      }
    };
  }

  <X> X eval(SnapshotAction.Algebra<S, V, R, X> interpreter);

  default <K, E> ProjectionAction<K, S, E, V, R> asProjectionA() {
    return this::eval;
  }
}
