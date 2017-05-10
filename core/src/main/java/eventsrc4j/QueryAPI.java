package eventsrc4j;

import fj.F;
import fj.F2;
import fj.data.Option;

import static eventsrc4j.SequenceQueries.Latest;
import static eventsrc4j.SnapshotStoreModes.Cache;
import static eventsrc4j.Snapshots.caseOf;
import static eventsrc4j.Snapshots.getSeq;
import static eventsrc4j.Snapshots.getView;
import static eventsrc4j.Steps.yield;

public final class QueryAPI<K, S, E, V> implements ProjectionAction.Factory<K, S, E, V> {

  private final F2<Event<K, S, E>, Snapshot<S, V>, Snapshot<S, V>> applyEvent;

  QueryAPI(F2<Event<K, S, E>, Snapshot<S, V>, Snapshot<S, V>> applyEvent) {
    this.applyEvent = applyEvent;
  }

  public static <K, S, E, V> QueryAPI<K, S, E, V> queryAPI(F<E, F<V, V>> applyEvent, V initialState) {
    return new QueryAPI<>((event, snapshot) -> event.apply(applyEvent, getView(snapshot).orSome(initialState)));
  }

  public ProjectionAction<K, S, E, V, Snapshot<S, V>> getLatestSnapshot() {

    return GetSnapshot(Latest()).bind(latestSnapshot ->
        caseOf(latestSnapshot)
            .Deleted_(Pure(latestSnapshot))
            .otherwise(() ->
                applyLatestEvents(latestSnapshot).bind(maybeNewSnapshot ->
                    maybeNewSnapshot.map(newSnapshot -> caseOf(newSnapshot)
                        .NoSnapshot_(Pure(newSnapshot))
                        .otherwise(() -> PutSnapshot(newSnapshot, Cache())))
                        .orSome(Pure(latestSnapshot)))
            )

    );
  }

  private ProjectionAction<K, S, E, V, Option<Snapshot<S, V>>> applyLatestEvents(Snapshot<S, V> latestSnapshot) {
    return ReadEventStream(getSeq(latestSnapshot),
        Fold.fold(latestSnapshot, (snapshot, event) -> yield(applyEvent.f(event, snapshot)), Option::some, Option::none));
  }
}
