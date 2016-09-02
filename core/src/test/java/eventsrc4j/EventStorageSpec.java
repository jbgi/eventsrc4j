package eventsrc4j;

import eventsrc4j.io.EventStorage;
import eventsrc4j.io.WritableEventStream;
import fj.test.Gen;
import fj.test.Property;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

import static eventsrc4j.WStreamAction.Read;
import static eventsrc4j.WStreamAction.Write;
import static fj.test.Property.prop;
import static fj.test.Property.property;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;

public final class EventStorageSpec<K, S, E> {

  private final Gen<K> keys;

  private final Gen<E> events;

  public EventStorageSpec(Gen<K> keys, Gen<E> events) {
    this.keys = keys;
    this.events = events;
  }

  public Property read_return_write(EventStorage<K, S, E> eventStorage) {
    return property(keys, events, key -> event -> {

      WritableEventStream<K, S, E> stream = eventStorage.stream(key);

      Optional<S> lastSeq =
          stream.read(Optional.empty(), s -> s.reduce((first, second) -> second))
              .runUnchecked()
              .map(Events::getSeq);

      WriteResult<K, S, E> writeResult = stream
          .write(lastSeq, Instant.EPOCH, singletonList(event))
          .runUnchecked();

      return prop(
          writeResult.events().map(
              writtenEvents -> writtenEvents.size() == 1
                  && writtenEvents.equals(stream
                  .read(lastSeq, s -> s.collect(toList()))
                  .runUnchecked())
          ).orElse(false)
      );
    });
  }

  public Property read_return_write_actions(
      Function<K, Predicate<WStreamAction<K, S, E, Boolean>>> actionInterpreter) {
    return property(keys, events, key -> event -> {

      WStreamAction<K, S, E, Optional<S>> lastSeqA = Read(Optional.empty(),
          s -> s.reduce((first, second) -> second).map(Events::getSeq));

      WStreamAction<K, S, E, Boolean> compareEventsA = lastSeqA.bind(lastSeq -> {

            WStreamAction<K, S, E, List<Event<K, S, E>>> writeEventsA =
                Write(lastSeq, Instant.EPOCH, Collections.singletonList(event),
                    writeResult -> writeResult.events().orElse(emptyList()));

            WStreamAction<K, S, E, List<Event<K, S, E>>> readEventsA = Read(lastSeq, s -> s.collect(toList()));

            return writeEventsA.bind(writtenEvents ->
                readEventsA.map(readEvents -> writtenEvents.size() == 1 && writtenEvents.equals(readEvents))
            );
          }
      );

      return prop(actionInterpreter.apply(key).test(compareEventsA));
    });
  }

  public Property concurrent_write_fails(EventStorage<K, S, E> eventStorage) {
    return property(keys, events, key -> event -> {

      WritableEventStream<K, S, E> stream = eventStorage.stream(key);

      Optional<S> lastSeq =
          stream.read(Optional.empty(), s -> s.reduce((first, second) -> second))
              .runUnchecked()
              .map(Events::getSeq);

      stream
          .write(lastSeq, Instant.EPOCH, singletonList(event))
          .runUnchecked();

      WriteResult<K, S, E> concurrentWrite = stream
          .write(lastSeq, Instant.EPOCH, singletonList(event))
          .runUnchecked();

      return prop(
          !concurrentWrite.events().isPresent()
      );
    });
  }
}
