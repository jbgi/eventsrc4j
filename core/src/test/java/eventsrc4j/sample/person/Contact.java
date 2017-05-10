package eventsrc4j.sample.person;

import eventsrc4j.data;
import org.derive4j.Data;
import org.derive4j.Flavour;

@data
public abstract class Contact {

  public abstract <R> R match(Cases<R> cases);

  interface Cases<R> {
    R byEmail(String email);

    R byPhone(String phoneNumber);

    R byMail(Address postalAddress);
  }


}
