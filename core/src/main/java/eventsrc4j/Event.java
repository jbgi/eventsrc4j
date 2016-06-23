package eventsrc4j;

import java.time.Instant;
import org.derive4j.Data;

import static eventsrc4j.Events.getDomainEvent;
import static eventsrc4j.Events.getSeq;
import static eventsrc4j.Events.getTime;

/**
 * Event wraps the event payload (domain event) with common information
 * (event sequence number and time of the event)
 */
@Data public abstract class Event<S, E> {

  public interface Case<S, E, R> {
    R Event(S seq, Instant time, E domainEvent);
  }

  Event() {}

  public abstract <R> R match(Case<S, E, R> event);

  public final S seq() {
    return getSeq(this);
  }

  public final Instant time() {
    return getTime(this);
  }

  public final E domainEvent() {
    return getDomainEvent(this);
  }

  @Override public abstract int hashCode();
  @Override public abstract boolean equals(Object obj);
  @Override public abstract String toString();

}
