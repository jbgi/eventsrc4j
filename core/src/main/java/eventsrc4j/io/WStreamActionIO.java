package eventsrc4j.io;

import eventsrc4j.Event;
import eventsrc4j.StreamReader;
import eventsrc4j.WStreamAction;
import eventsrc4j.WriteResult;
import java.time.Instant;
import java.util.Optional;
import java.util.function.Function;

public interface WStreamActionIO<K, S, E, R> extends PureIO<R>, WStreamAction.Algebra<K, S, E, R, IO<R>> {

  static <K, S, E, R> WStreamActionIO<K, S, E, R> ioAlgebra(WritableEventStream<K, S, E> eventStream) {
    return new WStreamActionIO<K, S, E, R>() {

      @Override public <Q> WStreamActionIO<K, S, E, Q> vary() {
        return ioAlgebra(eventStream);
      }

      @Override public IO<R> Write(Optional<S> expectedSeq, Instant time, Iterable<E> events,
          Function<WriteResult<K, S, E>, R> withResult) {
        return eventStream.write(expectedSeq, time, events).map(withResult);
      }

      @Override public IO<R> Read(Optional<S> fromSeq, StreamReader<K, S, E, R> streamReader) {
        return eventStream.read(fromSeq, streamReader);
      }

      @Override public IO<R> Latest(Function<Optional<Event<K, S, E>>, R> eventReader) {
        return eventStream.latest().map(eventReader);
      }
    };
  }

  @Override default <Q> IO<R> Bind(WStreamAction<K, S, E, Q> action,
      Function<Q, WStreamAction<K, S, E, R>> function) {
    return action.eval(vary()).flatMap(q -> function.apply(q).eval(this));
  }

  <Q> WStreamActionIO<K, S, E, Q> vary();
}
