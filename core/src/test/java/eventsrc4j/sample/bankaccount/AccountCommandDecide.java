package eventsrc4j.sample.bankaccount;

import java.math.BigDecimal;
import java.util.function.Function;

import static eventsrc4j.sample.bankaccount.AccountCommandDecisions.Accepted;
import static eventsrc4j.sample.bankaccount.AccountCommandDecisions.Refused;
import static eventsrc4j.sample.bankaccount.AccountCommandRefusedReasons.AccountAlreadyOpened;
import static eventsrc4j.sample.bankaccount.AccountCommandRefusedReasons.AccountUnopened;
import static eventsrc4j.sample.bankaccount.AccountCommandRefusedReasons.InsufficientFunds;
import static eventsrc4j.sample.bankaccount.AccountEvents.Credited;
import static eventsrc4j.sample.bankaccount.AccountEvents.Opened;
import static eventsrc4j.sample.bankaccount.AccountEvents.Overdrawn;
import static eventsrc4j.sample.bankaccount.AccountEvents.Withdrawn;
import static eventsrc4j.sample.bankaccount.OpenedAccounts.ifSufficientDeposit;
import static java.util.Arrays.asList;

public final class AccountCommandDecide
    implements AccountCommand.Cases<Function<AccountState, AccountCommandDecision>> {

  @Override
  public Function<AccountState, AccountCommandDecision> Open(
      String accountNumber, Amount initialDeposit, BigDecimal minBalance) {

    return AccountStates.cases()
        // Command is only valid on unopened account
        .Unopened(
            // initialDeposit must be > minBalance:
            ifSufficientDeposit(initialDeposit, minBalance)
                .map(__ -> Accepted(Opened(accountNumber, initialDeposit, minBalance)))

                .orElse(Refused(InsufficientFunds()))
        )
        .otherwise(
            Refused(AccountAlreadyOpened())
        );
  }

  @Override
  public Function<AccountState, AccountCommandDecision> Withdraw(Amount amount) {

    return AccountStates.cases()
        // Command is only valid on opened account
        .Opened(account ->
            // can be withdrawn without hitting min balance ?
            account.tryWithdraw(amount)
                .map(withdrawnAccount -> // yes!
                    withdrawnAccount.hasPositiveBalance()
                        ? Accepted(Withdrawn(amount))
                        // but negative balance: let's trigger interests with an Overdrawn event!
                        : Accepted(asList(Withdrawn(amount), Overdrawn()))
                )
                // sorry, no...
                .orElse(Refused(InsufficientFunds()))
        )
        .otherwise(
            Refused(AccountUnopened())
        );
  }

  @Override
  public Function<AccountState, AccountCommandDecision> Credit(Amount amount) {

    return AccountStates.cases()
        // Command is only valid on opened account
        .Opened(
            Accepted(Credited(amount))
        )
        .otherwise(
            Refused(AccountUnopened())
        );
  }
}
