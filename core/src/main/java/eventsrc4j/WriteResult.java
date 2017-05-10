package eventsrc4j;

import fj.data.List;
import fj.data.Option;
import org.derive4j.Derive;

import static eventsrc4j.WriteResults.getEvents;

@data
@Derive({})
public abstract class WriteResult<K, S, E> {

  WriteResult() {
  }

  public interface Cases<K, S, E, X> {
    X Success(List<Event<K, S, E>> events);

    X DuplicateEventSeq();
  }

  public abstract <X> X match(Cases<K, S, E, X> cases);

  public final Option<List<Event<K, S, E>>> events() {
    return getEvents(this);
  }

  @Override
  public abstract String toString();

  @Override
  public abstract boolean equals(Object obj);

  @Override
  public abstract int hashCode();

}
