package eventsrc4j.sample.bankaccount;

import eventsrc4j.data;
import fj.data.List;
import org.derive4j.ExportAsPublic;

import static fj.data.List.single;

@data
public abstract class AccountCommandDecision {

  AccountCommandDecision(){}

  public interface Cases<R> {
    R Refuse(AccountCommandRefusedReason reason);
    R Accept(List<AccountEvent> events);
  }

  @ExportAsPublic
  static AccountCommandDecision Accept(AccountEvent event) {
    return AccountCommandDecisions.Accept(single(event));
  }


  public abstract <R> R match(Cases<R> cases);

  @Override
  public abstract int hashCode();
  @Override
  public abstract boolean equals(Object obj);
  @Override
  public abstract String toString();


}
