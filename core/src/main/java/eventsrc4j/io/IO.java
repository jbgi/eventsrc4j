package eventsrc4j.io;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.function.Function;

/**
 * An IO action. A recipe to produce a value, possibly via a side effect
 * @param <A> type of the returned value.
 */
@FunctionalInterface
public interface IO<A> {

  A run() throws IOException;

  default A runUnchecked() throws UncheckedIOException {
    try {
      return run();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  default <B> IO<B> map(Function<A, B> f) {
    return () -> f.apply(IO.this.run());
  }

  default <B> IO<B> bind(Function<A, IO<B>> f) {
    return () -> f.apply(IO.this.run()).run();
  }
}

