package eventsrc4j.io;

/**
 * Implementations of this interface deal with persisting snapshots so that they don't need to be recomputed every time.
 * Specifically, implementations do NOT deal with generating snapshots, only storing/retrieving any persisted snapshot.
 *
 * @param <K> The type of the key for snapshots. This does not need to be the same as for the event stream itself.
 * @param <V> The type of the value wrapped by Snapshots that this store persists.
 */
public interface SnapshotStorage<K, S, V> {

  /**
   * Access a specific snapshot stream.
   * @param key
   * @return access api for the specific key.
   */
  SnapshotStream<S, V> snapshots(K key);

}
