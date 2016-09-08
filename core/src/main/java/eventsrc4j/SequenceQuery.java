package eventsrc4j;

import org.derive4j.Data;

@Data
public abstract class SequenceQuery<S> {
  public interface Cases<S, R> {
    R Before(S s);

    R Earliest();

    R Latest();
  }

  SequenceQuery() {
  }


  public abstract <R> R match(Cases<S, R> cases);

  @Override
  public abstract int hashCode();

  @Override
  public abstract boolean equals(Object obj);

  @Override
  public abstract String toString();
}
