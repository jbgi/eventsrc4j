package eventsrc4j;

import java.util.Comparator;

/**
 * A mathematical sequence.
 *
 * forall a. first <= a
 * forall a. next(a) > a
 */
public interface Sequence<S> extends Comparator<S> {

  S first();

  S next(S seq);

  Sequence<Long> longs = new Sequence<Long>() {

    public int compare(Long seq1, Long seq2) {
      return seq1.compareTo(seq2);
    }

    public Long first() {
      return 1L;
    }

    public Long next(Long seq) {
      return seq + 1;
    }
  };
}
