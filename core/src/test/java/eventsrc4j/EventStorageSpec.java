package eventsrc4j;

import eventsrc4j.io.EventStorage;
import eventsrc4j.io.WritableEventStream;
import fj.data.List;
import fj.data.Option;
import fj.test.Gen;
import fj.test.Property;
import java.time.Instant;
import java.util.function.Function;
import java.util.function.Predicate;

import static fj.data.List.nil;
import static fj.data.List.single;
import static fj.data.Option.none;
import static fj.test.Property.prop;
import static fj.test.Property.property;
import static java.util.Collections.singletonList;

public final class EventStorageSpec<K, S, E> implements WStreamAction.Factory<K, S, E> {

  private final Gen<K> keys;

  private final Gen<E> events;

  public EventStorageSpec(Gen<K> keys, Gen<E> events) {
    this.keys = keys;
    this.events = events;
  }

  public Property read_return_write(EventStorage<K, S, E> eventStorage) {
    return property(keys, events, key -> event -> {

      WritableEventStream<K, S, E> stream = eventStorage.stream(key);

      Option<S> lastSeq =
          stream.read(none(), Fold.last())
              .runUnchecked()
              .map(Events::getSeq);

      WriteResult<K, S, E> writeResult = stream
          .write(lastSeq, Instant.EPOCH, singletonList(event))
          .runUnchecked();

      return prop(
          writeResult.events().map(
              writtenEvents -> writtenEvents.length() == 1
                  && writtenEvents.equals(stream
                  .read(lastSeq, Fold.toList())
                  .runUnchecked())
          ).orSome(false)
      );
    });
  }

  public Property read_return_write_actions(
      Function<K, Predicate<WStreamAction<K, S, E, Boolean>>> actionInterpreter) {
    return property(keys, events, key -> event -> {

      WStreamAction<K, S, E, Option<S>> lastSeqA = ReadEventStream(Option.none(), Fold.last())
          .map(last -> last.map(Events::getSeq));

      WStreamAction<K, S, E, Boolean> compareEventsA = lastSeqA.bind(lastSeq -> {

            WStreamAction<K, S, E, List<Event<K, S, E>>> writeEventsA =
                WriteEvents(lastSeq, Instant.EPOCH, single(event))
                    .map(WriteResults::getEvents)
                    .map(es -> es.orSome(nil()));

            WStreamAction<K, S, E, List<Event<K, S, E>>> readEventsA = ReadEventStream(lastSeq, Fold.toList());

            return writeEventsA.bind(writtenEvents ->
                readEventsA.map(readEvents -> writtenEvents.length() == 1 && writtenEvents.equals(readEvents))
            );
          }
      );

      return prop(actionInterpreter.apply(key).test(compareEventsA));
    });
  }

  public Property concurrent_write_fails(EventStorage<K, S, E> eventStorage) {
    return property(keys, events, key -> event -> {

      WritableEventStream<K, S, E> stream = eventStorage.stream(key);

      Option<S> lastSeq =
          stream.read(none(), Fold.last())
              .runUnchecked()
              .map(Events::getSeq);

      stream
          .write(lastSeq, Instant.EPOCH, single(event))
          .runUnchecked();

      WriteResult<K, S, E> concurrentWrite = stream
          .write(lastSeq, Instant.EPOCH, single(event))
          .runUnchecked();

      return prop(concurrentWrite.events().isNone());
    });
  }
}
