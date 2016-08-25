package eventsrc4j.memory;

import java.util.function.BiFunction;
import org.derive4j.Data;
import org.derive4j.FieldNames;

@Data
public abstract class GlobalSeq<S> {

  GlobalSeq() {
  }

  public abstract <R> R match(@FieldNames({"globalSeq", "seq"}) BiFunction<S, S, R> seq);

  public final S globalSeq() {
    return GlobalSeqs.getGlobalSeq(this);
  }

  public final S seq() {
    return GlobalSeqs.getSeq(this);
  }

  @Override
  public abstract int hashCode();

  @Override
  public abstract boolean equals(Object obj);

  @Override
  public abstract String toString();
}
