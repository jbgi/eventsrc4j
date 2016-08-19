package eventsrc4j;

import static eventsrc4j.Snapshots.*;

import java.time.Instant;
import java.util.Optional;

import org.derive4j.Data;

/**
 * A Snapshot wraps an optional value, and tags it with an event Id. We can say a 'snapshot' of key K at event
 * S is a value V. The value is somehow generated from the event stream (see QueryApi): it is a view (or reduction) of the event stream for K
 * up to the sequence S.
 *
 * @tparam V The type of the value wrapped by the Snapshot
 */
@Data
public abstract class Snapshot<S, V> {

    public interface Cases<S, V, R> {

        /**
         * Events have been saved and there is a value stored.
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
         * @param seq Represents the point in the stream where the deletion occured.
         */
        R Deleted(S seq, Instant time);

        /**
         * Events have been saved but due to some events being unknown, the view could not be safely computed.
         * This is used to prevent taking decision based on a wrong or partial view.
         */
        R Unavailable();
    }

    Snapshot() {
    }

    public abstract <R> R match(Cases<S, V, R> cases);

    public final Optional<S> seq() {
        return getSeq(this);
    }

    public final Optional<Instant> time() {
        return getTime(this);
    }

    public final Optional<V> view() {
        return getView(this);
    }

    @Override
    public abstract int hashCode();

    @Override
    public abstract boolean equals(Object obj);

    @Override
    public abstract String toString();
}
