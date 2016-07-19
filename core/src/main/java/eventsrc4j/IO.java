package eventsrc4j;

import java.io.IOException;

@FunctionalInterface
public interface IO<A> {
    
    A run() throws IOException;
    
}
