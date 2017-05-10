package eventsrc4j;

/**
 * Mode of storage of a given snapshot: either as the earliest point in the event stream (Epoch) or not (Cache)
 */
@data
public abstract class SnapshotStoreMode {
  SnapshotStoreMode(){}

  public interface Cases<R> {
    R Epoch();
    R Cache();
  }

  public abstract <R> R match(Cases<R> cases);

  @Override
  public abstract int hashCode();

  @Override
  public abstract boolean equals(Object obj);

  @Override
  public abstract String toString();

}
