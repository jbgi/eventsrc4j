package eventsrc4j.io;

import eventsrc4j.SequenceQuery;
import eventsrc4j.Snapshot;
import eventsrc4j.SnapshotStoreMode;

/**
 * Implementations of this interface deal with persisting snapshots for a specific key.
 *
 * @param <V> The type of the value wrapped by Snapshots that this store persists.
 */
public interface SnapshotStream<S, V> {

  /**
   * Retrieve a snapshot before the given sequence number. We typically specify a sequence number if we want to get
   * some old snapshot i.e. the latest persisted snapshot may have been generated after the point in time that we're
   * interested in.
   *
   * @param sequence What sequence we want to get the snapshot for (earliest snapshot, latest, or latest before some sequence)
   * @return The snapshot, a NoSnapshot if there was no snapshot for the given conditions.
   */
  IO<Snapshot<S, V>> get(SequenceQuery<S> sequence);

  /**
   * Save a given snapshot
   * @param snapshot The snapshot to save
   * @param mode Defines whether the given snapshot should be deemed the earliest point in the event stream (Epoch) or not (Cache)
   * @return the saved snapshot.
   */
  IO<Snapshot<S, V>> put(Snapshot<S, V> snapshot, SnapshotStoreMode mode);

}
