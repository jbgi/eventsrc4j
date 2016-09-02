package eventsrc4j.sample.bankaccount;

import java.math.BigDecimal;
import java.util.function.Function;

import static eventsrc4j.sample.bankaccount.AccountCommandDecisions.Accepted;
import static eventsrc4j.sample.bankaccount.AccountCommandDecisions.Refused;
import static eventsrc4j.sample.bankaccount.AccountCommandRefusedReasons.AccountAlreadyOpened;
import static eventsrc4j.sample.bankaccount.AccountCommandRefusedReasons.AccountUnopened;
import static eventsrc4j.sample.bankaccount.AccountCommandRefusedReasons.InsufficientFunds;
import static eventsrc4j.sample.bankaccount.AccountEvents.Created;
import static eventsrc4j.sample.bankaccount.AccountEvents.Credited;
import static eventsrc4j.sample.bankaccount.AccountEvents.Overdrawn;
import static eventsrc4j.sample.bankaccount.AccountEvents.Withdrawn;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

public final class AccountCommandDecide
    implements AccountCommand.Cases<Function<AccountState, AccountCommandDecision>> {

  @Override
  public Function<AccountState, AccountCommandDecision> Open(
      String accountNumber, Amount initialDeposit, BigDecimal minBalance) {

    return AccountStates.cases()

        .Unopened(() -> OpenedAccounts.Open(initialDeposit, minBalance)

            .map(__ -> Accepted(
                singletonList(Created(accountNumber, initialDeposit, minBalance))))

            .orElse(Refused(InsufficientFunds())))

        .otherwise(Refused(AccountAlreadyOpened()));
  }

  @Override
  public Function<AccountState, AccountCommandDecision> Withdraw(Amount amount) {

    return AccountStates.cases()

        .Opened(openedAccount -> openedAccount.withdraw(amount)

            .map(newValidState -> newValidState.balance().compareTo(BigDecimal.ZERO) < 0
                ? asList(Withdrawn(amount), Overdrawn())
                : singletonList(Withdrawn(amount)))
            .map(AccountCommandDecisions::Accepted)

            .orElse(Refused(InsufficientFunds())))

        .otherwise(Refused(AccountUnopened()));
  }

  @Override
  public Function<AccountState, AccountCommandDecision> Credit(Amount amount) {

    return AccountStates.cases()

        .Opened(__ -> Accepted(singletonList(Credited(amount))))

        .otherwise(Refused(AccountUnopened()));
  }
}
