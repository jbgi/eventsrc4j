package eventsrc4j;

import java.util.function.Function;
import java.util.stream.Stream;

@FunctionalInterface
public interface EventStreamConsumer<S, E, R> extends Function<Stream<Event<S, E>>, R> {

    @Override
    R apply(Stream<Event<S, E>> eventStream);
}
