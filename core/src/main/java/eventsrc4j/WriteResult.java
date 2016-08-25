package eventsrc4j;

import java.util.List;
import java.util.Optional;
import org.derive4j.Data;

import static eventsrc4j.WriteResults.getEvents;

@Data
public abstract class WriteResult<K, S, E> {

  WriteResult() {
  }

  public interface Cases<K, S, E, X> {
    X Success(List<Event<K, S, E>> events);

    X DuplicateEventSeq();
  }

  public abstract <X> X match(Cases<K, S, E, X> cases);

  public final Optional<List<Event<K, S, E>>> events() {
    return getEvents(this);
  }

  @Override
  public abstract String toString();

  @Override
  public abstract boolean equals(Object obj);

  @Override
  public abstract int hashCode();
}
