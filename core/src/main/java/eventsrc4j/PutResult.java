package eventsrc4j;

import org.derive4j.Data;

@Data
public abstract class PutResult {

    PutResult(){}

    public interface Cases<X> {
        X Success();
        X DuplicateEventSeq();
    }

    public abstract <X> X match(Cases<X> cases);

    @Override
    public abstract String toString();

    @Override
    public abstract boolean equals(Object obj);

    @Override
    public abstract int hashCode();
    
}
