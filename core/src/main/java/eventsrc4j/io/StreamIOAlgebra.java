package eventsrc4j.io;

import eventsrc4j.Event;
import eventsrc4j.StreamAction;
import eventsrc4j.StreamReader;
import java.util.Optional;
import java.util.function.Function;

public interface StreamIOAlgebra<K, S, E, R> extends PureIO<R>, StreamAction.Algebra<K, S, E, R, IO<R>> {

  static <K, S, E, R> StreamIOAlgebra<K, S, E, R> of(EventStream<K, S, E> eventStream) {

    return new StreamIOAlgebra<K, S, E, R>() {

      @Override public <Q> StreamIOAlgebra<K, S, E, Q> vary() {
        return of(eventStream);
      }

      @Override public IO<R> Read(Optional<S> fromSeq, StreamReader<K, S, E, R> streamReader) {
        return eventStream.read(fromSeq, streamReader);
      }

      @Override public IO<R> Latest(Function<Optional<Event<K, S, E>>, R> eventReader) {
        return eventStream.latest().map(eventReader);
      }
    };
  }

  @Override default <Q> IO<R> Bind(StreamAction<K, S, E, Q> action, Function<Q, StreamAction<K, S, E, R>> function) {
    return action.eval(vary()).flatMap(q -> function.apply(q).eval(this));
  }

  <Q> StreamIOAlgebra<K, S, E, Q> vary();
}
