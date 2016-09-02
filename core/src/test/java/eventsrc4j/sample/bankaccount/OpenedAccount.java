package eventsrc4j.sample.bankaccount;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.function.Function;
import org.derive4j.Data;
import org.derive4j.Derive;
import org.derive4j.ExportAsPublic;
import org.derive4j.Visibility;

import static eventsrc4j.sample.bankaccount.OpenedAccounts.AccountState0;
import static eventsrc4j.sample.bankaccount.OpenedAccounts.getBalance;
import static eventsrc4j.sample.bankaccount.OpenedAccounts.getMinBalance;
import static eventsrc4j.sample.bankaccount.OpenedAccounts.modBalance0;
import static eventsrc4j.sample.bankaccount.OpenedAccounts.setBalance0;
import static java.math.BigDecimal.ZERO;

@Data(@Derive(withVisibility = Visibility.Smart))
public abstract class OpenedAccount {

  public interface Cases<R> {
    R AccountState(BigDecimal balance, BigDecimal minBalance);
  }

  OpenedAccount() {
  }

  @ExportAsPublic
  static Optional<OpenedAccount> ifSufficientDeposit(Amount initialDeposit, BigDecimal minBalance) {

    return minBalance.compareTo(initialDeposit.value()) <= 0
        ? Optional.of(AccountState0(initialDeposit.value(), minBalance))
        : Optional.empty();
  }

  public Optional<OpenedAccount> tryWithdraw(Amount amount) {

    BigDecimal newBalance = balance().subtract(amount.value());

    return minBalance().compareTo(newBalance) <= 0
        ? Optional.of(setBalance0(newBalance).apply(this))
        : Optional.empty();
  }

  public OpenedAccount credit(Amount amount) {
    return modBalance0(b -> b.add(amount.value())).apply(this);
  }

  public boolean hasPositiveBalance() {
    return ZERO.compareTo(balance()) <= 0;
  }

  public final BigDecimal balance() {
    return getBalance(this);
  }

  public final BigDecimal minBalance() {
    return getMinBalance(this);
  }

  public abstract <R> R match(Cases<R> cases);

  public final <R> R match(Function<OpenedAccount, R> cases) {
    return cases.apply(this);
  }

  @Override
  public abstract int hashCode();

  @Override
  public abstract boolean equals(Object obj);

  @Override
  public abstract String toString();
}
