package eventsrc4j.io;

import eventsrc4j.StreamAction;
import java.util.function.Function;

public interface StreamActionIO<K, S, E, R> extends PureIO<R>, StreamAction.Algebra<K, S, E, R, IO<R>> {

  @Override default <Q> IO<R> Bind(StreamAction<K, S, E, Q> action,
      Function<Q, StreamAction<K, S, E, R>> function) {
    return action.eval(vary()).flatMap(q -> function.apply(q).eval(this));
  }

  <Q> StreamActionIO<K, S, E, Q> vary();
}
