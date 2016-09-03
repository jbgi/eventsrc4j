package eventsrc4j.sample.bankaccount;

import java.math.BigDecimal;
import java.util.function.Function;

import static eventsrc4j.sample.bankaccount.AccountCommandDecisions.Accept;
import static eventsrc4j.sample.bankaccount.AccountCommandDecisions.Refuse;
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
                .map(__ -> Accept(Opened(accountNumber, initialDeposit, minBalance)))

                .orElse(Refuse(InsufficientFunds()))
        )
        .otherwise(
            Refuse(AccountAlreadyOpened())
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
                        ? Accept(Withdrawn(amount))
                        // but negative balance: let's trigger interests with an Overdrawn event!
                        : Accept(asList(Withdrawn(amount), Overdrawn()))
                )
                // sorry, no...
                .orElse(Refuse(InsufficientFunds()))
        )
        .otherwise(
            Refuse(AccountUnopened())
        );
  }

  @Override
  public Function<AccountState, AccountCommandDecision> Credit(Amount amount) {

    return AccountStates.cases()
        // Command is only valid on opened account
        .Opened(
            Accept(Credited(amount))
        )
        .otherwise(
            Refuse(AccountUnopened())
        );
  }
}
