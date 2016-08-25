package eventsrc4j;

import java.time.Instant;
import java.util.function.Function;
import org.derive4j.Data;

import static eventsrc4j.Events.Event;
import static eventsrc4j.Events.getDomainEvent;
import static eventsrc4j.Events.getKey;
import static eventsrc4j.Events.getSeq;
import static eventsrc4j.Events.getTime;

/**
 * Event wraps the event payload (domain event) with common information
 * (event sequence number and time of the event)
 */
@Data
public abstract class Event<K, S, E> {

  public interface Case<K, S, E, R> {
    R Event(K key, S seq, Instant time, E domainEvent);
  }

  Event() {
  }

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
   *
   * @return
   */
  public final Instant time() {
    return getTime(this);
  }

  public final E domainEvent() {
    return getDomainEvent(this);
  }

  @Override
  public abstract int hashCode();

  @Override
  public abstract boolean equals(Object obj);

  @Override
  public abstract String toString();

  public static <K, S, E, KK, SS, EE> Function<Event<K, S, E>, Event<KK, SS, EE>> mapKSE(
      Function<K, KK> kk, Function<S, SS> ss, Function<E, EE> ee) {
    return Events.<K, S, E>cases()
        .Event((key, seq, time, domainEvent) -> Event(kk.apply(key), ss.apply(seq), time,
            ee.apply(domainEvent)));
  }
}
