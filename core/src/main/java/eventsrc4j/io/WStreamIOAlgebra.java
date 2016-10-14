package eventsrc4j.io;

import eventsrc4j.Event;
import eventsrc4j.StreamReader;
import eventsrc4j.WStreamAction;
import eventsrc4j.WriteResult;
import java.time.Instant;
import java.util.Optional;
import java.util.function.Function;

public interface WStreamIOAlgebra<K, S, E, R> extends PureIO<R>, WStreamAction.Algebra<K, S, E, R, IO<R>> {

  static <K, S, E, R> WStreamIOAlgebra<K, S, E, R> of(WritableEventStream<K, S, E> eventStream) {
    return new WStreamIOAlgebra<K, S, E, R>() {

      @Override public <Q> WStreamIOAlgebra<K, S, E, Q> vary() {
        return of(eventStream);
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
    return action.eval(vary()).bind(q -> function.apply(q).eval(this));
  }

  <Q> WStreamIOAlgebra<K, S, E, Q> vary();
}
