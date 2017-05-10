package eventsrc4j.sample.bankaccount;

import eventsrc4j.data;
import java.math.BigDecimal;
import java.util.Optional;
import org.derive4j.Derive;
import org.derive4j.ExportAsPublic;
import org.derive4j.Visibility;

import static eventsrc4j.sample.bankaccount.Amounts.Amount0;
import static eventsrc4j.sample.bankaccount.Amounts.getValue;

/**
 * A strictly positive amount of money.
 */
@data
@Derive(withVisibility = Visibility.Smart)
public abstract class Amount {
  Amount() {}

  public interface Case<R> {
    R Amount(BigDecimal value);
  }

  public abstract <R> R match(Case<R> cases);

  @ExportAsPublic
  static Optional<Amount> Amount(BigDecimal value) {
    return BigDecimal.ZERO.compareTo(value) < 0
        ? Optional.of(Amount0(value))
        : Optional.empty();
  }

  public final BigDecimal value() {
    return getValue(this);
  }

  @Override
  public abstract int hashCode();

  @Override
  public abstract boolean equals(Object obj);

  @Override
  public abstract String toString();


}
