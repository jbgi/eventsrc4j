package eventsrc4j.sample.bankaccount;

import java.util.function.Function;

import static eventsrc4j.sample.bankaccount.AccountStates.Opened;
import static eventsrc4j.sample.bankaccount.AccountStates.modOpenedAccount;
import static eventsrc4j.sample.bankaccount.OpenedAccounts.AccountState0;
import static eventsrc4j.sample.bankaccount.OpenedAccounts.modBalance0;

public abstract class AccountEventApply {

  public static final Function<AccountEvent, Function<AccountState, AccountState>> ApplyEvent =

      AccountEvents.cases()
          .<Function<AccountState, AccountState>>Opened((initialDeposit, minBalance) ->
              __ -> Opened(AccountState0(initialDeposit.value(), minBalance)))

          .Withdrawn((Amount amount) ->
              modOpenedAccount(modBalance0(balance -> balance.subtract(amount.value()))))

          .Credited((Amount amount) ->
              modOpenedAccount(modBalance0(balance -> balance.add(amount.value()))))

          .Overdrawn(Function.identity());

  private AccountEventApply() {
  }
}
