package eventsrc4j.sample.bankaccount;

import eventsrc4j.*;
import eventsrc4j.io.*;
import eventsrc4j.memory.MemoryEventStorage;
import eventsrc4j.memory.MemorySnapshotStorage;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static eventsrc4j.sample.bankaccount.AccountCommands.Credit;
import static eventsrc4j.sample.bankaccount.AccountCommands.Open;
import static eventsrc4j.sample.bankaccount.AccountCommands.Withdraw;
import static eventsrc4j.sample.bankaccount.AccountEventApply.ApplyEvent;
import static eventsrc4j.sample.bankaccount.AccountNumbers.AccountNumber;
import static eventsrc4j.sample.bankaccount.Amounts.Amount;
import static java.util.stream.Collectors.toList;

public final class AccountCommandService {

    private final EventStorage<AccountNumber, Long, AccountEvent> eventStorage;

    private final SnapshotStorage<AccountNumber, Long, AccountState> snapshotStorage;

    private final CommandAPI<AccountCommand, AccountNumber, Long, AccountEvent, AccountState, AccountCommandRefusedReason>
            commandApi;

    public AccountCommandService(EventStorage<AccountNumber, Long, AccountEvent> eventStorage,
                                 SnapshotStorage<AccountNumber, Long, AccountState> snapshotStorage) {
        this.eventStorage = eventStorage;
        this.snapshotStorage = snapshotStorage;
        this.commandApi = CommandAPI.commandAPI(new AccountCommandDecide(), AccountStates.Unopened(), ApplyEvent,
                AccountCommandRefusedReasons.AccountClosed());
    }

    public IO<CommandDecision<AccountCommandRefusedReason, Event<AccountNumber, Long, AccountEvent>>> execute(
            AccountNumber accountNumber, AccountCommand command) {

        return commandApi.handleAction(Instant.now(), command)
                .eval(
                        ESActionIOAlgebra.of(
                                SnapshotIOAlgebra.of(snapshotStorage.snapshots(accountNumber)),
                                WStreamIOAlgebra.of(eventStorage.stream(accountNumber)))
                );
    }

    public static void main(String[] args) throws IOException {
        MemoryEventStorage<AccountNumber, Long, AccountEvent> memoryEventStorage = new MemoryEventStorage<>(Sequence.longs);
        MemorySnapshotStorage<AccountNumber, Long, AccountState> snapshotStorage = new MemorySnapshotStorage<>(Sequence.longs);
        AccountCommandService accountCommandService = new AccountCommandService(memoryEventStorage, snapshotStorage);

        AccountNumber accountNumber = AccountNumber(1);
        AccountCommand openAccountCmd = Open(Amount(BigDecimal.TEN).get(), BigDecimal.ZERO.subtract(BigDecimal.TEN));
        IO<CommandDecision<AccountCommandRefusedReason, Event<AccountNumber, Long, AccountEvent>>> openAccount = accountCommandService.execute(accountNumber, openAccountCmd);
        IO<CommandDecision<AccountCommandRefusedReason, Event<AccountNumber, Long, AccountEvent>>> openAccount2 = accountCommandService.execute(AccountNumber(2), openAccountCmd);
        IO<CommandDecision<AccountCommandRefusedReason, Event<AccountNumber, Long, AccountEvent>>> withdraw = accountCommandService.execute(accountNumber, Withdraw(Amount(new BigDecimal("20")).get()));
        IO<CommandDecision<AccountCommandRefusedReason, Event<AccountNumber, Long, AccountEvent>>> deposit = accountCommandService.execute(accountNumber, Credit(Amount(new BigDecimal("30")).get()));

        System.out.println(
                openAccount
                        .bind(__ -> withdraw)
                        .bind(__ -> withdraw)
                        .bind(__ -> deposit)
                        .bind(__ -> openAccount)
                        .bind(__ -> memoryEventStorage.stream(accountNumber).read(Optional.empty(), s -> s.collect(toList())))
                        .bind(__ -> snapshotStorage.snapshots(accountNumber).get(SequenceQueries.Before(3L)))
                        .run());
    }
}
