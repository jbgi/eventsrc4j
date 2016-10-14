package eventsrc4j;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.derive4j.Data;
import org.derive4j.ExportAsPublic;

import static eventsrc4j.CommandDecisions.Refuse;
import static java.util.Collections.singletonList;

@Data
public abstract class CommandDecision<R, E> {

  public interface Cases<R, E, X> {
    X Refuse(R reason);

    X Accept(List<E> events);
  }

  public interface AcceptIfPresent<E> {
    <R> CommandDecision<R, E> elseRefuseFor(R reason);
  }

  CommandDecision() {
  }

  @ExportAsPublic
  static <R,E> CommandDecision<R, E> Accept(E event) {
    return CommandDecisions.Accept(singletonList(event));
  }


  @ExportAsPublic
  static <E> AcceptIfPresent<E> ifEvent(Optional<E> event) {
    return ifEvents(event.map(Collections::singletonList));
  }

  @ExportAsPublic
  static <E> AcceptIfPresent<E> ifEvents(Optional<List<E>> event) {
    return new AcceptIfPresent<E>() {
      @Override public <R> CommandDecision<R, E> elseRefuseFor(R reason) {
        return event.map(e -> CommandDecisions.<R, E>Accept(e)).orElse(Refuse(reason));
      }
    };
  }

  public abstract <X> X match(Cases<R, E, X> cases);

  @Override
  public abstract int hashCode();

  @Override
  public abstract boolean equals(Object obj);

  @Override
  public abstract String toString();
}