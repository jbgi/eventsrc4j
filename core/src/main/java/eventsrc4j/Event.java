package eventsrc4j;

import static eventsrc4j.Events.*;

import java.time.Instant;

import org.derive4j.Data;

/**
 * Event wraps the event payload (domain event) with common information
 * (event sequence number and time of the event)
 */
@Data public abstract class Event<K, S, E> {

  public interface Case<K, S, E, R> {
    R Event(K key, S seq, S globalSeq, Instant time, E domainEvent);
  }

  Event() {}

  public abstract <R> R match(Case<K, S, E, R> event);
  
  public final K key() {
    return getKey(this);  
  }

  /**
   * Sequence number of the event (relative to given key)
   */
  public final S seq() {
    return getSeq(this);
  }

  /**
   * Global sequence number of the event (cross-key)
   */
  public final S globalSeq() {
    return getGlobalSeq(this);
  }

  /**
   * 
   * @return
   */
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
