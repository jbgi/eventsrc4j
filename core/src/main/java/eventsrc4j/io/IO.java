package eventsrc4j.io;

import fj.F;
import java.io.IOException;
import java.io.UncheckedIOException;

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

  default <B> IO<B> map(F<A, B> f) {
    return () -> f.f(IO.this.run());
  }

  default <B> IO<B> bind(F<A, IO<B>> f) {
    return () -> f.f(IO.this.run()).run();
  }

  static <A> IO<A> io(IO<A> io) {
    return io;
  }
}

