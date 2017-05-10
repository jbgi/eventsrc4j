package eventsrc4j;

import fj.data.List;
import fj.data.Option;
import org.derive4j.ExportAsPublic;

import static eventsrc4j.CommandDecisions.Refuse;
import static fj.data.List.single;

@data
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
    return CommandDecisions.Accept(single(event));
  }


  @ExportAsPublic
  static <E> AcceptIfPresent<E> ifEvent(Option<E> event) {
    return ifEvents(event.map(List::single));
  }

  @ExportAsPublic
  static <E> AcceptIfPresent<E> ifEvents(Option<List<E>> event) {
    return new AcceptIfPresent<E>() {
      @Override public <R> CommandDecision<R, E> elseRefuseFor(R reason) {
        return event.map(CommandDecisions::<R, E>Accept).orSome(Refuse(reason));
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