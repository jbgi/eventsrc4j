package eventsrc4j.sample.bankaccount;

import eventsrc4j.CommandAPI;
import eventsrc4j.CommandDecision;
import eventsrc4j.Event;
import eventsrc4j.io.ESActionIOAlgebra;
import eventsrc4j.io.EventStorage;
import eventsrc4j.io.SnapshotIOAlgebra;
import eventsrc4j.io.SnapshotStorage;
import eventsrc4j.io.WStreamIOAlgebra;
import java.io.IOException;
import java.time.Instant;

import static eventsrc4j.sample.bankaccount.AccountEventApply.ApplyEvent;

public class AccountCommandService {

  private final EventStorage<AccountId, Long, AccountEvent> eventStorage;

  private final SnapshotStorage<AccountId, Long, AccountState> snapshotStorage;

  private final CommandAPI<AccountCommand, AccountId, Long, AccountEvent, AccountState, AccountCommandRefusedReason>
      commandApi;

  public AccountCommandService(EventStorage<AccountId, Long, AccountEvent> eventStorage,
      SnapshotStorage<AccountId, Long, AccountState> snapshotStorage) {
    this.eventStorage = eventStorage;
    this.snapshotStorage = snapshotStorage;
    this.commandApi = CommandAPI.commandAPI(new AccountCommandDecide(), AccountStates.Unopened(), ApplyEvent,
        AccountCommandRefusedReasons.AccountClosed());
  }

  CommandDecision<AccountCommandRefusedReason, Event<AccountId, Long, AccountEvent>> execute(
      AccountId accountId, AccountCommand command) throws IOException {

    return commandApi.handleAction(Instant.now(), command)
        .eval(
            ESActionIOAlgebra.of(
                SnapshotIOAlgebra.of(snapshotStorage.snapshots(accountId)),
                WStreamIOAlgebra.of(eventStorage.stream(accountId)))
        )
        .run();
  }
}
