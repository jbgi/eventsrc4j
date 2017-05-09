package eventsrc4j;

import eventsrc4j.util.Streams;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

import static eventsrc4j.SequenceQueries.Latest;
import static eventsrc4j.SnapshotAction.Get;
import static eventsrc4j.SnapshotStoreModes.Cache;
import static eventsrc4j.Snapshots.caseOf;
import static eventsrc4j.Snapshots.getSeq;
import static eventsrc4j.Snapshots.getView;

public final class QueryAPI<K, S, E, V> {

  private final BiFunction<Event<K, S, E>, Snapshot<S, V>, Snapshot<S, V>> applyEvent;

  QueryAPI(BiFunction<Event<K, S, E>, Snapshot<S, V>, Snapshot<S, V>> applyEvent) {
    this.applyEvent = applyEvent;
  }

  public ProjectionAction<K, S, E, V, Snapshot<S, V>> getLatest() {

    return Get(Latest(), Function.<Snapshot<S, V>>identity()).<K, E>asProjectionA().bind(latestSnapshot ->
            caseOf(latestSnapshot)
                .Deleted_(ProjectionAction.<K, S, E, V, Snapshot<S, V>>Pure(latestSnapshot))
                .otherwise(() ->
                    applyLatestEvents(latestSnapshot).<V>asProjectionA().bind(maybeNewSnapshot ->
                        maybeNewSnapshot.map(
                            snapshot -> SnapshotAction.Put(snapshot, Cache()).<K, E>asProjectionA())
                            .orElse(ProjectionAction.Pure(latestSnapshot)))
                )

        );
  }

  private StreamAction<K, S, E, Optional<Snapshot<S, V>>> applyLatestEvents(Snapshot<S, V> latestSnapshot) {

    return StreamAction.Read(getSeq(latestSnapshot),
        eventStream -> Streams.takeWhile(
            Snapshots.<S, V>cases()
                .Deleted_(false)
                .otherwise_(true)::apply,
            Streams.scanLeft(applyEvent, latestSnapshot, eventStream))
            .reduce((s1, s2) -> s2));
  }

  public static <K, S, E, V> QueryAPI<K, S, E, V> queryAPI(Function<E, Function<V, V>> applyEvent, V intialState) {
    return new QueryAPI<>((event, snapshot) -> event.apply(applyEvent, getView(snapshot).orElse(intialState)));
  }
}
