package eventsrc4j;

import fj.F;
import fj.data.List;
import java.time.Instant;

import static eventsrc4j.CommandDecisions.caseOf;
import static eventsrc4j.QueryAPI.queryAPI;
import static eventsrc4j.Snapshots.getSeq;
import static eventsrc4j.WriteResults.caseOf;

public class CommandAPI<C, K, S, E, V, R> implements ESAction.Factory<K, S, E, V> {

  private final F<C, F<Snapshot<S, V>, CommandDecision<R, E>>> decideFunction;

  private final QueryAPI<K, S, E, V> queryAPI;

  CommandAPI(F<C, F<Snapshot<S, V>, CommandDecision<R, E>>> decideFunction,
      QueryAPI<K, S, E, V> queryAPI) {
    this.decideFunction = decideFunction;
    this.queryAPI = queryAPI;
  }

  public static <C, K, S, E, V, R> CommandAPI<C, K, S, E, V, R> commandAPI(
      F<C, F<V, CommandDecision<R, E>>> decideFunction,
      V initialState,
      F<E, F<V, V>> applyEvent,
      R snapshotDeleteErrorReason) {

    return new CommandAPI<>(c -> {
      F<V, CommandDecision<R, E>> decideForState = decideFunction.f(c);

      return Snapshots.<S, V>cases()
          .Value((seq, time, view) -> decideForState.f(view))
          .NoSnapshot(() -> decideForState.f(initialState))
          .Deleted_(CommandDecisions.Refuse(snapshotDeleteErrorReason));
    }, queryAPI(applyEvent, initialState));
  }

  public ESAction<K, S, E, V, CommandDecision<R, Event<K, S, E>>> handleAction(Instant time, C command) {
    return handleAction(time, decideFunction.f(command));
  }

  public ESAction<K, S, E, V, CommandDecision<R, Event<K, S, E>>> handleAction(Instant time,
      F<Snapshot<S, V>, CommandDecision<R, E>> decisionF) {

    return ESAction(queryAPI.getLatestSnapshot()).bind(latestSnapshot ->

        caseOf(decisionF.f(latestSnapshot))

            .Refuse(r ->
                Pure(CommandDecisions.<R, Event<K, S, E>>Refuse(r)))

            .Accept((List<E> domainEvents) ->

                WriteEvents(getSeq(latestSnapshot), time, domainEvents)
                    .bind(writeResult -> caseOf(writeResult)
                        .Success(events -> Pure(CommandDecisions.<R, Event<K, S, E>>Accept(events)))
                        .DuplicateEventSeq(() -> handleAction(time, decisionF))
                    )

            ));
  }
}
