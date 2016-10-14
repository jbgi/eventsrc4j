package eventsrc4j.memory;

import eventsrc4j.SequenceQuery;
import eventsrc4j.Snapshot;
import eventsrc4j.SnapshotStoreMode;
import eventsrc4j.io.IO;
import eventsrc4j.io.SnapshotStorage;
import eventsrc4j.io.SnapshotStream;

public final class MemorySnapshotStorage<K, S, V> implements SnapshotStorage<K, S, V> {
  @Override public SnapshotStream<S, V> snapshots(K key) {
    return new SnapshotStream<S, V>() {



      @Override public IO<Snapshot<S, V>> get(SequenceQuery<S> sequence) {
        throw new UnsupportedOperationException("TODO");
      }

      @Override public IO<Snapshot<S, V>> put(Snapshot<S, V> snapshot, SnapshotStoreMode mode) {
        throw new UnsupportedOperationException("TODO");
      }
    };
  }
}
