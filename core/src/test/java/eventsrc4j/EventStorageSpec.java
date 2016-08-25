package eventsrc4j;

import fj.test.Gen;
import fj.test.Property;
import java.time.Instant;
import java.util.Optional;

import static fj.test.Property.prop;
import static fj.test.Property.property;
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

      Optional<S> lastSeq =
          eventStorage.stream(key).read(Optional.empty(), s -> s.reduce((first, second) -> second))
              .runUnchecked()
              .map(Events::getSeq);

      WriteResult<K, S, E> writeResult = eventStorage.stream(key)
          .write(lastSeq, Instant.EPOCH, singletonList(event))
          .runUnchecked();

      return prop(
          writeResult.events().map(
              writtenEvents -> writtenEvents.size() == 1
                  && writtenEvents.equals(eventStorage.stream(key)
                  .read(lastSeq, s -> s.collect(toList()))
                  .runUnchecked())
          ).orElse(false)
      );
    });
  }

  public Property concurrent_write_fails(EventStorage<K, S, E> eventStorage) {
    return property(keys, events, key -> event -> {

      Optional<S> lastSeq =
          eventStorage.stream(key).read(Optional.empty(), s -> s.reduce((first, second) -> second))
              .runUnchecked()
              .map(Events::getSeq);

      eventStorage.stream(key)
          .write(lastSeq, Instant.EPOCH, singletonList(event))
          .runUnchecked();

      WriteResult<K, S, E> concurrentWrite = eventStorage.stream(key)
          .write(lastSeq, Instant.EPOCH, singletonList(event))
          .runUnchecked();

      return prop(
          !concurrentWrite.events().isPresent()
      );
    });
  }
}
