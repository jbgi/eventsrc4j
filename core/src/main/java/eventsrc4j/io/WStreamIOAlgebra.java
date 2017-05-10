package eventsrc4j.io;

import eventsrc4j.Event;
import eventsrc4j.Fold;
import eventsrc4j.WStreamAction;
import eventsrc4j.WriteResult;
import fj.F;
import fj.data.Option;
import java.time.Instant;
import org.derive4j.hkt.TypeEq;

public interface WStreamIOAlgebra<K, S, E, R> extends PureIO<R>, WStreamAction.Algebra<K, S, E, R, IO<R>> {

  static <K, S, E, R> WStreamIOAlgebra<K, S, E, R> of(WritableEventStream<K, S, E> eventStream) {
    return new WStreamIOAlgebra<K, S, E, R>() {

      @Override public IO<R> Read(Option<S> fromSeq, Fold<Event<K, S, E>, R> streamFold) {
        return eventStream.read(fromSeq, streamFold);
      }

      @Override public IO<R> Latest(TypeEq<Option<Event<K, S, E>>, R> resultType) {
        return eventStream.latest().map(resultType::coerce);
      }

      @Override public IO<R> Write(Option<S> expectedSeq, Instant time, Iterable<E> events, TypeEq<WriteResult<K, S, E>, R> resultType) {
        return eventStream.write(expectedSeq, time, events).map(resultType::coerce);
      }

      @Override public <Q> WStreamIOAlgebra<K, S, E, Q> vary() {
        return of(eventStream);
      }

    };
  }

  @Override default <Q> IO<R> BindW(WStreamAction<K, S, E, Q> action,
      F<Q, WStreamAction<K, S, E, R>> function) {
    return action.eval(vary()).bind(q -> function.f(q).eval(this));
  }

  <Q> WStreamIOAlgebra<K, S, E, Q> vary();
}
