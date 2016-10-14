package eventsrc4j.sample.bankaccount;

import eventsrc4j.CommandDecision;
import eventsrc4j.CommandDecisions;
import java.math.BigDecimal;
import java.util.function.Function;

import static eventsrc4j.CommandDecisions.Refuse;
import static eventsrc4j.CommandDecisions.ifEvent;
import static eventsrc4j.CommandDecisions.ifEvents;
import static eventsrc4j.sample.bankaccount.AccountCommandRefusedReasons.AccountAlreadyOpened;
import static eventsrc4j.sample.bankaccount.AccountCommandRefusedReasons.AccountUnopened;
import static eventsrc4j.sample.bankaccount.AccountCommandRefusedReasons.InsufficientFunds;
import static eventsrc4j.sample.bankaccount.AccountEvents.Credited;
import static eventsrc4j.sample.bankaccount.AccountEvents.Opened;
import static eventsrc4j.sample.bankaccount.AccountEvents.Overdrawn;
import static eventsrc4j.sample.bankaccount.AccountEvents.Withdrawn;
import static eventsrc4j.sample.bankaccount.OpenedAccounts.ifSufficientDeposit;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

public final class AccountCommandDecide
    implements AccountCommand.Cases<Function<AccountState, CommandDecision<AccountCommandRefusedReason, AccountEvent>>> {

  @Override
  public Function<AccountState, CommandDecision<AccountCommandRefusedReason, AccountEvent>> Open(
      AccountNumber accountNumber, Amount initialDeposit, BigDecimal minBalance) {

    return AccountStates.cases()
        // Command is only valid on unopened account
        .Unopened(
            // initialDeposit must be > minBalance:
            ifEvent(
                ifSufficientDeposit(initialDeposit, minBalance)
                    .map(__ -> Opened(accountNumber, initialDeposit, minBalance)))

                .elseRefuseFor(InsufficientFunds()))

        .otherwise(
            Refuse(AccountAlreadyOpened()));
  }

  @Override
  public Function<AccountState, CommandDecision<AccountCommandRefusedReason, AccountEvent>> Withdraw(Amount amount) {

    return AccountStates.cases()
        // Command is only valid on opened account
        .Opened(account ->
            // can be withdrawn without hitting min balance ?
            ifEvents(
                account.tryWithdraw(amount)
                    .map(withdrawnAccount -> // yes!
                        withdrawnAccount.hasPositiveBalance()
                            ? singletonList(Withdrawn(amount))
                            // but negative balance: let's trigger interests with an Overdrawn event!
                            : asList(Withdrawn(amount), Overdrawn())))
                // sorry, no...
                .elseRefuseFor(InsufficientFunds()))

        .otherwise(
            Refuse(AccountUnopened()));
  }

  @Override
  public Function<AccountState, CommandDecision<AccountCommandRefusedReason, AccountEvent>> Credit(Amount amount) {

    return AccountStates.cases()
        // Command is only valid on opened account
        .Opened(
            CommandDecisions.<AccountCommandRefusedReason, AccountEvent>Accept(Credited(amount)))

        .otherwise(
            Refuse(AccountUnopened()));
  }
}
