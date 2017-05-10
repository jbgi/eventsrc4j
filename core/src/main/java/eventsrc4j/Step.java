package eventsrc4j;

import fj.F;

@data
public abstract class Step<R, S> {
  public interface Cases<R, S, X> {
    X done(R value);
    X yield(S stepper);
  }
  public abstract <X> X match(Cases<R, S, X> cases);

  public final <S2> Step<R, S2> bind(F<S, Step<R, S2>> f) {
    return match(Steps.cases(Steps::done, f));
  }
}
