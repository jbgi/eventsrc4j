package eventsrc4j;

import java.util.function.Function;
import java.util.stream.Stream;

@FunctionalInterface
public interface StreamReader<K, S, E, R> extends Function<Stream<Event<K, S, E>>, R> {

    @Override
    R apply(Stream<Event<K, S, E>> eventStream);
}
