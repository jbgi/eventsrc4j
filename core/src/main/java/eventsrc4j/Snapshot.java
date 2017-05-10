package eventsrc4j;

import fj.Equal;
import fj.Hash;
import fj.Ord;
import fj.Show;
import fj.data.Option;
import java.time.Instant;

import static eventsrc4j.Snapshots.getSeq;
import static eventsrc4j.Snapshots.getTime;
import static eventsrc4j.Snapshots.getView;

/**
 * A Snapshot wraps an optional value, and tags it with an event Id. We can say a 'snapshot' of key
 * K at event S is a value V. The value is somehow generated from the event stream (see QueryApi):
 * it is a view (or reduction) of the event stream for K up to the sequence S.
 *
 * @tparam V The type of the value wrapped by the Snapshot
 */
@data
public abstract class Snapshot<S, V> {

  public interface Cases<S, V, R> {

    /**
     * Events have been saved and there is a value stored.
     *
     * @param seq the point in the stream that this Snapshot is for.
     * @param view the view on the stream upto to that point.
     */
    R Value(S seq, Instant time, V view);

    /**
     * There is no snapshot... i.e. no events have been saved.
     */
    R NoSnapshot();

    /**
     * Events have been saved and there is no value (i.e. the value has been deleted).
     *
     * @param seq Represents the point in the stream where the deletion occured.
     */
    R Deleted(S seq, Instant time);
  }

  Snapshot() {
  }

  public abstract <R> R match(Cases<S, V, R> cases);

  public final Option<S> seq() {
    return getSeq(this);
  }

  public final Option<Instant> time() {
    return getTime(this);
  }

  public final Option<V> view() {
    return getView(this);
  }

  @Override
  public abstract int hashCode();

  @Override
  public abstract boolean equals(Object obj);

  @Override
  public abstract String toString();

  static final Equal<Instant> instantEqual = Equal.anyEqual();
  static final Hash<Instant> instantHash = Hash.anyHash();
  static final Show<Instant> instantShow = Show.anyShow();
  static final Ord<Instant> instantOrd = Ord.comparableOrd();
}
