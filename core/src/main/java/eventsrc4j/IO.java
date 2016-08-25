package eventsrc4j;

import java.io.IOException;
import java.io.UncheckedIOException;

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
}

