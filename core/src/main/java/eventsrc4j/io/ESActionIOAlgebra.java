package eventsrc4j.io;

import eventsrc4j.ESAction;
import fj.F;

public interface ESActionIOAlgebra<K, S, E, V, R> extends PureIO<R>, ESAction.DelegatingAlgebra<K, S, E, V, R, IO<R>> {


  static <K, S, E, V, R> ESActionIOAlgebra<K, S, E, V, R> of(SnapshotIOAlgebra<S, V, R> snapshotIOAlgebra,
      WStreamIOAlgebra wStreamIOAlgebra) {
    return new ESActionIOAlgebra<K, S, E, V, R>() {

      @Override public SnapshotIOAlgebra<S, V, R> snapshotAlgebra() {
        return snapshotIOAlgebra;
      }

      @Override public WStreamIOAlgebra<K, S, E, R> wStreamAlgebra() {
        return wStreamIOAlgebra;
      }
    };
  }

  @Override default <Q> IO<R> BindES(ESAction<K, S, E, V, Q> action,
      F<Q, ESAction<K, S, E, V, R>> function) {
    return action.eval(vary()).bind(q -> function.f(q).eval(this));
  }

  @Override SnapshotIOAlgebra<S, V, R> snapshotAlgebra();

  @Override WStreamIOAlgebra<K, S, E, R> wStreamAlgebra();

  default <Q> ESActionIOAlgebra<K, S, E, V, Q> vary() {
    return of(snapshotAlgebra().vary(), wStreamAlgebra().vary());
  }
}
