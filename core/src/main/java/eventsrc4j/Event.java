package eventsrc4j;

import fj.Equal;
import fj.F;
import fj.Hash;
import fj.Ord;
import fj.Show;
import java.time.Instant;

import static eventsrc4j.Events.Event;
import static eventsrc4j.Events.getDomainEvent;
import static eventsrc4j.Events.getKey;
import static eventsrc4j.Events.getSeq;
import static eventsrc4j.Events.getTime;
import static eventsrc4j.Snapshots.Value;

/**
 * Event wraps the event payload (domain event) with common information
 * (event sequence number and time of the event)
 */
@data
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

  public static <K, S, E, KK, SS, EE> F<Event<K, S, E>, Event<KK, SS, EE>> mapKSE(
      F<K, KK> kk, F<S, SS> ss, F<E, EE> ee) {
    return Events.<K, S, E>cases()
        .Event((key, seq, time, domainEvent) -> Event(kk.f(key), ss.f(seq), time,
            ee.f(domainEvent)));
  }

  public <V> Snapshot<S, V> apply(F<E, F<V, V>> applyEventFunction, V previousState) {
    return this.match((key, seq, time, domainEvent) -> Value(seq, time, applyEventFunction.f(domainEvent).f(previousState)));
  }

  static final Equal<Instant> instantEqual = Equal.anyEqual();
  static final Hash<Instant> instantHash = Hash.anyHash();
  static final Show<Instant> instantShow = Show.anyShow();
  static final Ord<Instant> instantOrd = Ord.comparableOrd();
}
