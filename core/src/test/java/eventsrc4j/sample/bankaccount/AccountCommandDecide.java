package eventsrc4j.sample.bankaccount;

import eventsrc4j.CommandDecision;
import eventsrc4j.CommandDecisions;
import fj.F;
import fj.data.List;
import java.math.BigDecimal;

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
import static fj.data.List.single;

public final class AccountCommandDecide
    implements AccountCommand.Cases<F<AccountState, CommandDecision<AccountCommandRefusedReason, AccountEvent>>> {

  @Override
  public F<AccountState, CommandDecision<AccountCommandRefusedReason, AccountEvent>> Open(
      Amount initialDeposit, BigDecimal minBalance) {

    return AccountStates.cases()
        // Command is only valid on unopened account
        .Unopened_(
            // initialDeposit must be > minBalance:
            ifEvent(
                ifSufficientDeposit(initialDeposit, minBalance)
                    .map(__ -> Opened(initialDeposit, minBalance)))

                .elseRefuseFor(InsufficientFunds()))

        .otherwise_(
            Refuse(AccountAlreadyOpened()));
  }

  @Override
  public F<AccountState, CommandDecision<AccountCommandRefusedReason, AccountEvent>> Withdraw(Amount amount) {

    return AccountStates.cases()
        // Command is only valid on opened account
        .Opened(account ->
            // can be withdrawn without hitting min balance ?
            ifEvents(
                account.tryWithdraw(amount)
                    .map(withdrawnAccount -> // yes!
                        withdrawnAccount.hasPositiveBalance()
                            ? single(Withdrawn(amount))
                            // but negative balance: let's trigger interests with an Overdrawn event!
                            : List.arrayList(Withdrawn(amount), Overdrawn())))
                // sorry, no...
                .elseRefuseFor(InsufficientFunds()))

        .otherwise_(
            Refuse(AccountUnopened()));
  }

  @Override
  public F<AccountState, CommandDecision<AccountCommandRefusedReason, AccountEvent>> Credit(Amount amount) {

    return AccountStates.cases()
        // Command is only valid on opened account
        .Opened_(
            CommandDecisions.<AccountCommandRefusedReason, AccountEvent>Accept(Credited(amount)))

        .otherwise_(
            Refuse(AccountUnopened()));
  }
}
