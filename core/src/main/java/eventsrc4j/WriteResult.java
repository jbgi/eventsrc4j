package eventsrc4j;

import java.util.function.Function;

import org.derive4j.Data;

@Data
public abstract class WriteResult {

    private static final Function<WriteResult, Boolean> isSuccess = WriteResults.cases()
            .Success(true)
            .otherwise(false);

    WriteResult() {
    }

    public interface Cases<X> {
        X Success();

        X DuplicateEventSeq();
    }

    public boolean successful() {
        return isSuccess.apply(this);
    }

    public abstract <X> X match(Cases<X> cases);

    @Override
    public abstract String toString();

    @Override
    public abstract boolean equals(Object obj);

    @Override
    public abstract int hashCode();

}
