package eventsrc4j;

import fj.F;
import fj.F2;
import fj.data.List;
import fj.data.Option;
import java.util.function.Supplier;

import static fj.Function.identity;

/**
 * Loosely based on http://blog.higher-order.com/blog/2010/10/14/scalaz-tutorial-enumeration-based-io-with-iteratees/
 */
public interface Fold<E, R> {

  interface Case<E, R, X> {
    <S> X fold(S init, F2<S, E, Step<R, S>> onElement, F<S, R> onEndOfStream, Supplier<R> onEmpty);
  }

  <X> X match(Case<E, R, X> fold);

  static <S, E, R> Fold<E, R> fold(S init, F2<S, E, Step<R, S>> onElement, F<S, R> onEndOfStream, Supplier<R> onEmpty) {
    return new Fold<E, R>() {
      @Override public <X> X match(Case<E, R, X> fold) {
        return fold.fold(init, onElement, onEndOfStream, onEmpty);
      }
    };
  }

  static <E> Fold<E, Option<E>> head() {
    return fold(Option.<E>none(), (s, e) -> Steps.done(Option.some(e)), identity(), Option::none);
  }

  static <E> Fold<E, Option<E>> last() {
    return fold(Option.<E>none(), (s, e) -> Steps.yield(Option.some(e)), identity(), Option::none);
  }

  static <E> Fold<E, List<E>> toList() {
    return fold(List.<E>nil(), (s, e) -> Steps.yield(List.cons(e, s)), List::reverse, List::nil);
  }


}