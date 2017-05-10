package eventsrc4j.io;

import eventsrc4j.SequenceQuery;
import eventsrc4j.Snapshot;
import eventsrc4j.SnapshotAction;
import eventsrc4j.SnapshotStoreMode;
import fj.F;
import org.derive4j.hkt.TypeEq;

public interface SnapshotIOAlgebra<S, V, R> extends PureIO<R>, SnapshotAction.Algebra<S, V, R, IO<R>> {

  static <S, V, R> SnapshotIOAlgebra<S, V, R> of(SnapshotStream<S, V> eventStream) {

    return new SnapshotIOAlgebra<S, V, R>() {

      @Override public IO<R> Get(SequenceQuery<S> sequence, TypeEq<Snapshot<S, V>, R> resultType) {
        return eventStream.get(sequence).map(resultType::coerce);
      }

      @Override public IO<R> Put(Snapshot<S, V> snapshot, SnapshotStoreMode mode, TypeEq<Snapshot<S, V>, R> resultType) {
        return eventStream.put(snapshot, mode).map(resultType::coerce);
      }

      @Override public <Q> SnapshotIOAlgebra<S, V, Q> vary() {
        return of(eventStream);
      }

    };
  }

  @Override default <Q> IO<R> Bind(SnapshotAction<S, V, Q> action, F<Q, SnapshotAction<S, V, R>> function) {
    return action.eval(vary()).bind(q -> function.f(q).eval(this));
  }

  <Q> SnapshotIOAlgebra<S, V, Q> vary();
}
