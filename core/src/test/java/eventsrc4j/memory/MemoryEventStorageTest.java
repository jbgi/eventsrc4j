package eventsrc4j.memory;

import eventsrc4j.EventStorageSpec;
import eventsrc4j.Sequence;
import fj.test.Arbitrary;
import fj.test.Property;
import fj.test.reflect.CheckParams;
import fj.test.runner.PropertyTestRunner;
import org.junit.runner.RunWith;

import static fj.test.Gen.elements;

@RunWith(PropertyTestRunner.class)
@CheckParams(maxSize = 10000, minSuccessful = 200)
public final class MemoryEventStorageTest {

  private static final EventStorageSpec<Integer, Long, MemoryEventStorageTest.Event> spec =
      new EventStorageSpec<>(Arbitrary.arbInteger, elements(Event.values()));

  public Property read_return_write() {
    return spec.read_return_write(new MemoryEventStorage<>(Sequence.longs));
  }

  public Property concurrent_write_fails() {
    return spec.concurrent_write_fails(new MemoryEventStorage<>(Sequence.longs));
  }

  public Property read_return_write_actions() {
    MemoryEventStorage<Integer, Long, Event> eventStorage = new MemoryEventStorage<>(Sequence.longs);

    return spec.read_return_write_actions(
        k -> action -> eventStorage.stream(k).evalWAction(action).runUnchecked());
  }

  enum Event {
    USER_CREATED, USERNAME_UPDATED, DISPLAY_NAME_UPDATED
  }
}
