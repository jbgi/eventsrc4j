package eventsrc4j.sample.bankaccount;

import eventsrc4j.data;
import java.math.BigDecimal;

@data
public abstract class AccountEvent {

  AccountEvent(){}

  public interface Cases<R> {
    R Opened(Amount initialDeposit, BigDecimal minBalance);

    R Withdrawn(Amount amount);

    R Credited(Amount amount);

    R Overdrawn();
  }

  public abstract <R> R match(Cases<R> cases);

  @Override
  public abstract int hashCode();
  @Override
  public abstract boolean equals(Object obj);
  @Override
  public abstract String toString();

}
