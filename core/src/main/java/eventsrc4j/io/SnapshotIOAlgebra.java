package eventsrc4j.io;

import eventsrc4j.Event;
import eventsrc4j.SequenceQuery;
import eventsrc4j.Snapshot;
import eventsrc4j.SnapshotAction;
import eventsrc4j.SnapshotStoreMode;
import eventsrc4j.StreamAction;
import eventsrc4j.StreamReader;
import java.util.Optional;
import java.util.function.Function;

public interface SnapshotIOAlgebra<S, V, R> extends PureIO<R>, SnapshotAction.Algebra<S, V, R, IO<R>> {

  static <S, V, R> SnapshotIOAlgebra<S, V, R> of(SnapshotStream<S, V> eventStream) {

    return new SnapshotIOAlgebra<S, V, R>() {

      @Override public IO<R> Get(SequenceQuery<S> sequence, Function<Snapshot<S, V>, R> snapshotReader) {
        return eventStream.get(sequence).map(snapshotReader);
      }

      @Override public IO<R> Put(Snapshot<S, V> snapshot, SnapshotStoreMode mode, Function<Snapshot<S, V>, R> id) {
        return eventStream.put(snapshot, mode).map(id);
      }

      @Override public <Q> SnapshotIOAlgebra<S, V, Q> vary() {
        return of(eventStream);
      }

    };
  }

  @Override default <Q> IO<R> Bind(SnapshotAction<S, V, Q> action, Function<Q, SnapshotAction<S, V, R>> function) {
    return action.eval(vary()).flatMap(q -> function.apply(q).eval(this));
  }

  <Q> SnapshotIOAlgebra<S, V, Q> vary();
}
