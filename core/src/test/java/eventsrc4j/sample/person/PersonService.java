package eventsrc4j.sample.person;

import fj.F;
import fj.Unit;
import fj.data.List;
import fj.data.NonEmptyList;
import fj.data.Option;
import fj.data.Validation;

import static eventsrc4j.sample.person.Addresses.Address;
import static eventsrc4j.sample.person.Addresses.modNumber;
import static eventsrc4j.sample.person.Contacts.getPostalAddress;
import static eventsrc4j.sample.person.Contacts.modPostalAddress;
import static eventsrc4j.sample.person.FirstNames.FirstName;
import static eventsrc4j.sample.person.LastNames.LastName;
import static eventsrc4j.sample.person.NameValues.NameValue0;
import static eventsrc4j.sample.person.PersonName.NameValue.parseName;
import static eventsrc4j.sample.person.PersonNames.PersonName;
import static eventsrc4j.sample.person.Persons.Person;
import static eventsrc4j.sample.person.Persons.getContact;
import static eventsrc4j.sample.person.Persons.modContact;
import static fj.Semigroup.nonEmptyListSemigroup;
import static fj.Unit.unit;
import static fj.data.Option.none;
import static fj.data.Option.some;
import static fj.data.Option.some_;
import static fj.data.Validation.success;

public class PersonService {

  public static void main(String[] args) {

    String stringFirstName = "Joe ";

    String stringLastName = " Black";

    System.out.println(updatePersonName(42, stringFirstName, "", stringLastName));

    // oups! there was a off my one error in the import process. We must increment all street numbers!!

    // Easy with Derive4J
    F<Person, Person> incrementStreetNumber = modContact(modPostalAddress(modNumber(number -> number + 1)));

    // correctedJoe is a copy of joe with the street number incremented:
    Person correctedJoe = findById(42).map(incrementStreetNumber).success();

    Option<Integer> newStreetNumber = getPostalAddress(getContact(correctedJoe)).map(Addresses::getNumber);

    System.out.println(newStreetNumber); // print "Optional[11]" !!
  }

  public static List<String> updatePersonName(long personId, String stringFirstName, String stringMiddleName, String stringLastName) {
    return
        // Validate input:
        validatePersonName(stringFirstName, stringMiddleName.isEmpty() ? none() : some(stringMiddleName), stringLastName).bind(
            // try to retrieve person from store
            newName -> findById(personId)
                // try to apply the command:
                .bind(person -> person.changeName(newName))
                // try to save the updated person
                .bind(updatedPerson -> savePerson(personId, updatedPerson)).nel())
            // return an emply list if no error or the list of errors:
            .validation(NonEmptyList::toList, u -> List.nil());
  }

  private static Validation<String, Unit> savePerson(long personId, Person person) {
    // actually save in your datastore...
    return success(unit());
  }

  private static Validation<String, Person> findById(long personId) {
    return success(Person(
        PersonName(
            FirstName(NameValue0("Joe")),
            none(),
            LastName(NameValue0("Black"))
        ),
        Contacts.byMail(
            Address(10, "Main St")
        ),
        none()
    ));
  }

  private static Validation<NonEmptyList<String>, PersonName> validatePersonName(
      String stringFirstName, Option<String> stringMiddleName, String stringLastName) {

    return
        // validate first name
        validateName(stringFirstName, "First name").map(FirstNames::FirstName).nel()
            .accumulate(nonEmptyListSemigroup(),

            // validate middle name if present
            stringMiddleName.map(s -> validateName(s, "Middle Name").map(MiddleNames::MiddleName).map(some_()))
                .orSome(Validation.success(none())).nel(),

            // validate last name
            validateName(stringLastName, "Last name").map(LastNames::LastName).nel(),

            // assemble all
            PersonNames::PersonName
        );
  }

  public static Validation<String, PersonName.NameValue> validateName(String name, String format) {
    return parseName(name).toValidation(format + " is not valid");
  }


}
