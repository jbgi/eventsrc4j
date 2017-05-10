package eventsrc4j.io;

import eventsrc4j.Event;
import eventsrc4j.Fold;
import eventsrc4j.StreamAction;
import fj.F;
import fj.data.Option;
import org.derive4j.hkt.TypeEq;

public interface StreamIOAlgebra<K, S, E, R> extends PureIO<R>, StreamAction.Algebra<K, S, E, R, IO<R>> {

  static <K, S, E, R> StreamIOAlgebra<K, S, E, R> of(EventStream<K, S, E> eventStream) {

    return new StreamIOAlgebra<K, S, E, R>() {

      @Override public IO<R> Read(Option<S> fromSeq, Fold<Event<K, S, E>, R> streamFold) {
        return eventStream.read(fromSeq, streamFold);
      }

      @Override public IO<R> Latest(TypeEq<Option<Event<K, S, E>>, R> resultType) {
        return eventStream.latest().map(resultType::coerce);
      }

      @Override public <Q> StreamIOAlgebra<K, S, E, Q> vary() {
        return of(eventStream);
      }
    };
  }

  @Override default <Q> IO<R> Bind(StreamAction<K, S, E, Q> action, F<Q, StreamAction<K, S, E, R>> function) {
    return action.eval(vary()).bind(q -> function.f(q).eval(this));
  }

  <Q> StreamIOAlgebra<K, S, E, Q> vary();
}
