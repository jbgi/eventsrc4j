package eventsrc4j.io;

import fj.F;

/**
 * A source of events.
 * Implementations wrap around an underlying data store (e.g. in-memory map, RDBMS, noSql store).
 *
 * @param <K> event streams keys.
 * @param <S> sequence used for ordering events in a stream.
 * @param <E> concrete domain events.
 */
public interface EventStorage<K, S, E> {

  /**
   * Access a specific stream.
   *
   * @param key the stream key
   * @return a readable and writable
   */
  WritableEventStream<K, S, E> stream(K key);

  default <KK, SS, EE> EventStorage<KK, SS, EE> xmap(F<K, KK> kk, F<KK, K> k,
      F<S, SS> ss, F<SS, S> s, F<E, EE> ee,
      F<EE, E> e) {

    //TODO
    throw new UnsupportedOperationException();
  }
}
