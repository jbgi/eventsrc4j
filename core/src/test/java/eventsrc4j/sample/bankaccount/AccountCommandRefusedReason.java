package eventsrc4j.sample.bankaccount;

import eventsrc4j.data;

@data
public abstract class AccountCommandRefusedReason {

  public interface Cases<R> {

    R AccountAlreadyOpened();

    R InsufficientFunds();

    R AccountUnopened();

    R AccountClosed();
  }

  AccountCommandRefusedReason() {
  }

  public abstract <R> R match(Cases<R> cases);

  @Override
  public abstract int hashCode();

  @Override
  public abstract boolean equals(Object obj);

  @Override
  public abstract String toString();
}
