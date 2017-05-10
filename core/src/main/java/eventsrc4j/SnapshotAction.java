package eventsrc4j;

import eventsrc4j.io.SnapshotIOAlgebra;
import fj.F;
import org.derive4j.hkt.TypeEq;

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

    X Get(SequenceQuery<S> sequence, TypeEq<Snapshot<S, V>, R> resultType);

    X Put(Snapshot<S, V> snapshot, SnapshotStoreMode mode, TypeEq<Snapshot<S, V>, R> resultType);

    <Q> X Bind(SnapshotAction<S, V, Q> action, F<Q, SnapshotAction<S, V, R>> function);
  }

  static <S, V> Factory<S, V> factory() {
    return new Factory<S, V>() {};
  }

  interface Factory<S, V> {

    default SnapshotAction<S, V, Snapshot<S, V>> GetSnapshot(SequenceQuery<S> sequence) {
      return new SnapshotAction<S, V, Snapshot<S, V>>() {
        @Override public <X> X eval(Algebra<S, V, Snapshot<S, V>, X> interpreter) {
          return interpreter.Get(sequence, TypeEq.refl());
        }
      };
    }

    default SnapshotAction<S, V, Snapshot<S, V>> PutSnapshot(Snapshot<S, V> snapshot, SnapshotStoreMode mode) {
      return new SnapshotAction<S, V, Snapshot<S, V>>() {
        @Override public <X> X eval(Algebra<S, V, Snapshot<S, V>, X> interpreter) {
          return interpreter.Put(snapshot, mode, TypeEq.refl());
        }
      };
    }

    default <R> SnapshotAction<S, V, R> PureSnapshotAction(R value) {
      return new SnapshotAction<S, V, R>() {
        @Override public <X> X eval(Algebra<S, V, R, X> interpreter) {
          return interpreter.Pure(value);
        }
      };
    }
  }


  default <Q> SnapshotAction<S, V, Q> bind(F<R, SnapshotAction<S, V, Q>> f) {
    return new SnapshotAction<S, V, Q>() {
      @Override public <X> X eval(Algebra<S, V, Q, X> interpreter) {
        return interpreter.Bind(SnapshotAction.this, f);
      }
    };
  }

  default <Q> SnapshotAction<S, V, Q> map(F<R, Q> f) {
    return bind(r -> SnapshotAction.<S, V>factory().PureSnapshotAction(f.f(r)));
  }


  <X> X eval(SnapshotAction.Algebra<S, V, R, X> interpreter);

}
