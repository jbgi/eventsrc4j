package eventsrc4j.io;

import eventsrc4j.Event;
import eventsrc4j.StreamAction;
import eventsrc4j.StreamReader;
import java.util.Optional;
import java.util.function.Function;

public interface StreamActionIO<K, S, E, R> extends StreamAction.Interpreter<K, S, E, R, IO<R>> {

  interface Algebra<K, S, E, R> extends PureIO<R>, StreamAction.Algebra<K, S, E, R, IO<R>> {

    <Q> Algebra<K, S, E, Q> vary();

    @Override default <Q> IO<R> Bind(StreamAction<K, S, E, Q> action,
        Function<Q, StreamAction<K, S, E, R>> function) {
      return action.eval(vary()).flatMap(q -> function.apply(q).eval(this));
    }
  }

  static <K, S, E, R> Algebra<K, S, E, R> ioAlgebra(StreamActionIO<K, S, E, R> actionInterpreter) {

    return new Algebra<K, S, E, R>() {

      @Override public <Q> Algebra<K, S, E, Q> vary() {
        return ioAlgebra(actionInterpreter.vary());
      }

      @Override public IO<R> Read(Optional<S> fromSeq, StreamReader<K, S, E, R> streamReader) {
        return actionInterpreter.Read(fromSeq, streamReader);
      }

      @Override public IO<R> Latest(Function<Optional<Event<K, S, E>>, R> eventReader) {
        return actionInterpreter.Latest(eventReader);
      }
    };
  }

  <Q> StreamActionIO<K, S, E, Q> vary();
}
