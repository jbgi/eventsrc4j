package eventsrc4j;

import fj.test.Gen;
import fj.test.Property;
import fj.test.reflect.CheckParams;
import fj.test.runner.PropertyTestRunner;
import java.time.Instant;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.runner.RunWith;

import static eventsrc4j.Events.Event;
import static eventsrc4j.GlobalSeqs.seq;
import static fj.test.Property.prop;
import static fj.test.Property.property;

@RunWith(PropertyTestRunner.class) @CheckParams(maxSize = 10000) public class EventStorageSpec<K, S, E> {

  private Supplier<EventStorage<K, S, E>> emptyEventStorage;
  private Sequence<S> sequence;
  private Gen<K> keys;
  private Gen<E> events;

  public EventStorageSpec(Supplier<EventStorage<K, S, E>> emptyEventStorage, Gen<K> keys, Sequence<S> sequence, Gen<E> events) {
    this.emptyEventStorage = emptyEventStorage;
    this.sequence = sequence;
    this.keys = keys;
    this.events = events;
  }

  public Property latest_from_empty_stream_is_absent() {
    return property(keys, key -> prop(!emptyEventStorage.get().latest(key).runUncheckd().isPresent()));
  }

  public Property latest_return_last_write() {
    return property(keys, events, key -> event -> {

      EventStorage<K, S, E> eventStorage = emptyEventStorage.get();

      WriteResult writeResult = eventStorage.write(key, Optional.empty(), Instant.EPOCH, Stream.of(event)).runUncheckd();

      return prop(writeResult.successful()).and(
          prop(eventStorage.latest(key).runUncheckd().map(e -> e.equals(Event(key, sequence.first(), Instant.EPOCH, event))).orElse(false)));
    });
  }

  public Property allLatest_return_last_write() {
    return property(keys, events, key -> event -> {

      EventStorage<K, S, E> eventStorage = emptyEventStorage.get();

      WriteResult writeResult = eventStorage.write(key, Optional.empty(), Instant.EPOCH, Stream.of(event)).runUncheckd();

      return prop(writeResult.successful()).and(
          prop(eventStorage.allLatest().runUncheckd().map(e -> e.equals(Event(key, seq(sequence.first(), sequence.first()), Instant.EPOCH, event))).orElse(false)));
    });
  }

}
