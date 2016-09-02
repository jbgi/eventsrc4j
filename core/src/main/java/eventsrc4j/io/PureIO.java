package eventsrc4j.io;

import eventsrc4j.Pure;

public interface PureIO<R> extends Pure<R, IO<R>> {

  @Override
  default IO<R> Pure(R value) {
    return () -> value;
  }
}
