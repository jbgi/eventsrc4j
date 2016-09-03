package eventsrc4j.sample.bankaccount;

import java.util.List;
import org.derive4j.Data;
import org.derive4j.ExportAsPublic;

import static java.util.Collections.singletonList;

@Data
public abstract class AccountCommandDecision {

  AccountCommandDecision(){}

  public interface Cases<R> {
    R Refuse(AccountCommandRefusedReason reason);
    R Accept(List<AccountEvent> events);
  }

  @ExportAsPublic
  static AccountCommandDecision Accept(AccountEvent event) {
    return AccountCommandDecisions.Accept(singletonList(event));
  }


  public abstract <R> R match(Cases<R> cases);

  @Override
  public abstract int hashCode();
  @Override
  public abstract boolean equals(Object obj);
  @Override
  public abstract String toString();


}
