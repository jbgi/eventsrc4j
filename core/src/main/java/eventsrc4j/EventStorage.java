package eventsrc4j;

import java.util.function.Function;

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

  default <KK, SS, EE> EventStorage<KK, SS, EE> xmap(Function<K, KK> kk, Function<KK, K> k,
      Function<S, SS> ss, Function<SS, S> s, Function<E, EE> ee,
      Function<EE, E> e) {

    //TODO
    throw new UnsupportedOperationException();
  }
}
