package eventsrc4j.memory;

import eventsrc4j.data;
import fj.F2;
import org.derive4j.FieldNames;

@data
public abstract class GlobalSeq<S> {

  GlobalSeq() {
  }

  public abstract <R> R match(@FieldNames({"globalSeq", "seq"}) F2<S, S, R> seq);

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
