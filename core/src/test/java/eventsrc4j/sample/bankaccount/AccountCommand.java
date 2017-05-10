package eventsrc4j.sample.bankaccount;

import eventsrc4j.data;
import fj.F;
import java.math.BigDecimal;

@data
public abstract class AccountCommand {

  AccountCommand(){}

  public interface Cases<R> extends F<AccountCommand, R> {
    @Override default R f(AccountCommand command) {
      return command.match(this);
    }

    R Open(Amount initialDeposit, BigDecimal minBalance);

    R Withdraw(Amount amount);

    R Credit(Amount amount);
  }

  public abstract <R> R match(Cases<R> cases);

  @Override
  public abstract int hashCode();
  @Override
  public abstract boolean equals(Object obj);
  @Override
  public abstract String toString();

}
