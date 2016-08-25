package eventsrc4j.util;

import java.util.Comparator;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public final class Streams {

  private Streams() {
  }

  public static <A> Stream<A> takeWhile(Predicate<A> p, Stream<A> as) {
    return StreamSupport.stream(new Spliterator<A>() {

      final Spliterator<A> spliterator = as.spliterator();

      boolean continueAdvance = true;

      @Override
      public boolean tryAdvance(Consumer<? super A> action) {
        return continueAdvance && spliterator.tryAdvance(a -> {
          if (continueAdvance = p.test(a)) {
            action.accept(a);
          }
        });
      }

      @Override
      public Spliterator<A> trySplit() {
        return null;
      }

      @Override
      public long estimateSize() {
        return continueAdvance ? spliterator.estimateSize() : 0L;
      }

      @Override
      public int characteristics() {
        return spliterator.characteristics() & (~Spliterator.SIZED);
      }

      @Override
      public Comparator<? super A> getComparator() {
        return spliterator.getComparator();
      }
    }, false);
  }

  public static <A> Stream<A> dropWhile(Predicate<A> p, Stream<A> as) {
    return StreamSupport.stream(new Spliterator<A>() {

      final Spliterator<A> spliterator = as.spliterator();

      boolean continueTestDrop = true;

      @Override
      public boolean tryAdvance(Consumer<? super A> action) {
        if (continueTestDrop) {
          while (spliterator.tryAdvance(a -> {
            if (!(continueTestDrop = p.test(a))) {
              action.accept(a);
            }
          }) && continueTestDrop) {
            ;
          }
          return !continueTestDrop;
        }
        return spliterator.tryAdvance(action);
      }

      @Override
      public void forEachRemaining(Consumer<? super A> action) {
        if (continueTestDrop) {
          if (tryAdvance(action)) {
            spliterator.forEachRemaining(action);
          }
        } else {
          spliterator.forEachRemaining(action);
        }
      }

      @Override
      public Spliterator<A> trySplit() {
        return null;
      }

      @Override
      public long estimateSize() {
        return spliterator.estimateSize();
      }

      @Override
      public int characteristics() {
        return spliterator.characteristics() & (~Spliterator.SIZED);
      }

      @Override
      public Comparator<? super A> getComparator() {
        return spliterator.getComparator();
      }
    }, false);
  }
}
