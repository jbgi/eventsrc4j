package eventsrc4j;

import org.derive4j.Data;

/**
 * Wraps how we can query snapshots by sequence number:
 *   - before S - we want snapshot before sequence number S (exclusive)
 *   - earliest - we want to earliest snapshot stored i.e. this would be the current epoch.
 *   - latest - we want the latest snapshot stored. This is the most common case.
 */
@Data
public abstract class SequenceQuery<S> {
  public interface Cases<S, R> {
    R Before(S s);

    R Earliest();

    R Latest();
  }

  SequenceQuery() {
  }


  public abstract <R> R match(Cases<S, R> cases);

  @Override
  public abstract int hashCode();

  @Override
  public abstract boolean equals(Object obj);

  @Override
  public abstract String toString();
}
