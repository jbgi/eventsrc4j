package eventsrc4j.sample.bankaccount;

import java.math.BigDecimal;
import java.util.function.Function;
import org.derive4j.Data;

@Data
public abstract class AccountCommand {

  AccountCommand(){}

  public interface Cases<R> extends Function<AccountCommand, R> {
    @Override default R apply(AccountCommand command) {
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
