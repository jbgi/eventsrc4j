package eventsrc4j;

import java.time.Instant;
import java.util.List;
import java.util.function.Function;

import static eventsrc4j.CommandDecisions.caseOf;
import static eventsrc4j.QueryAPI.queryAPI;
import static eventsrc4j.Snapshots.getSeq;
import static eventsrc4j.WriteResults.caseOf;

public class CommandAPI<C, K, S, E, V, R> {

  private final Function<C, Function<Snapshot<S, V>, CommandDecision<R, E>>> decideFunction;

  private final QueryAPI<K, S, E, V> queryAPI;

  CommandAPI(Function<C, Function<Snapshot<S, V>, CommandDecision<R, E>>> decideFunction,
      QueryAPI<K, S, E, V> queryAPI) {
    this.decideFunction = decideFunction;
    this.queryAPI = queryAPI;
  }

  public static <C, K, S, E, V, R> CommandAPI<C, K, S, E, V, R> commandAPI(
      Function<C, Function<V, CommandDecision<R, E>>> decideFunction,
      V initialState,
      Function<E, Function<V, V>> applyEvent,
      R snapshotDeleteErrorReason) {
    return new CommandAPI<>(c -> {
      Function<V, CommandDecision<R, E>> decideForState = decideFunction.apply(c);
      return Snapshots.<S, V>cases()
          .Value((seq, time, view) -> decideForState.apply(view))
          .NoSnapshot(() -> decideForState.apply(initialState))
          .Deleted_(CommandDecisions.Refuse(snapshotDeleteErrorReason));
    }, queryAPI(applyEvent, initialState));
  }

  public ESAction<K, S, E, V, CommandDecision<R, Event<K, S, E>>> handleAction(Instant time, C command) {
    return handleAction(time, decideFunction.apply(command));
  }

  public ESAction<K, S, E, V, CommandDecision<R, Event<K, S, E>>> handleAction(Instant time,
      Function<Snapshot<S, V>, CommandDecision<R, E>> decisionF) {

    return queryAPI.getLatest().asESAction().bind(latestSnapshot ->

        caseOf(decisionF.apply(latestSnapshot))

            .Refuse(r ->
                ESAction.<K, S, E, V, CommandDecision<R, Event<K, S, E>>>Pure(CommandDecisions.Refuse(r)))

            .Accept((List<E> domainEvents) ->

                WStreamAction.Write(getSeq(latestSnapshot), time, domainEvents,
                    Function.<WriteResult<K, S, E>>identity()).<V>asESAction()
                    .bind(writeResult -> caseOf(writeResult)
                            .Success(events -> ESAction.<K, S, E, V, CommandDecision<R, Event<K, S, E>>>Pure(
                                CommandDecisions.Accept(events)))
                            .DuplicateEventSeq(() -> handleAction(time, decisionF))
                    )

            ));
  }
}
