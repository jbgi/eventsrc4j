package eventsrc4j.memory;

import eventsrc4j.EventStorageSpec;
import eventsrc4j.Sequence;
import fj.test.Arbitrary;
import fj.test.Gen;
import fj.test.Property;
import fj.test.reflect.CheckParams;
import fj.test.runner.PropertyTestRunner;
import org.junit.runner.RunWith;

import static fj.test.Gen.elements;

@RunWith(PropertyTestRunner.class)
@CheckParams(maxSize = 10000, minSuccessful = 200)
public class MemoryEventStorageTest {

  private static final EventStorageSpec<Integer, Long, MemoryEventStorageTest.Event> spec =
      new EventStorageSpec<>(() -> new MemoryEventStorage<>(Sequence.longs), Arbitrary.arbInteger, Sequence.longs, elements(Event.values()));

  enum Event {
    USER_CREATED, USERNAME_UPDATED, DISPLAY_NAME_UPDATED
  }

  public Property latest_from_empty_stream_is_absent() {
    return spec.latest_from_empty_stream_is_absent();
  }

  public Property latest_return_last_write() {
    return spec.latest_return_last_write();
  }


  public Property allLatest_return_last_write() {
    return spec.allLatest_return_last_write();
  }


}
