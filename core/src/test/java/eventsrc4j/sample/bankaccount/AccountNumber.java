package eventsrc4j.sample.bankaccount;

import org.derive4j.Data;

@Data
public abstract class AccountNumber {
    AccountNumber(){}
    public interface Case<X> {
        X AccountNumber(long value);
    }
    public abstract <X> X match(Case<X> Case);

    @Override
    public abstract int hashCode();
    @Override
    public abstract boolean equals(Object obj);
    @Override
    public abstract String toString();
}
