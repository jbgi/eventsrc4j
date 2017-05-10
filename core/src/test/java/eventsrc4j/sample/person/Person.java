/*
 * Copyright (c) 2015, Jean-Baptiste Giraudeau <jb@giraudeau.info>
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *  * Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *  * Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *  * Neither the name of the copyright holder nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package eventsrc4j.sample.person;

import eventsrc4j.data;
import fj.Equal;
import fj.Hash;
import fj.Ord;
import fj.Show;
import fj.data.Option;
import fj.data.Validation;
import java.time.LocalDate;
import java.time.chrono.ChronoLocalDate;

import static eventsrc4j.sample.person.Persons.getDateOfDecease;
import static eventsrc4j.sample.person.Persons.setPersonName;
import static fj.data.Validation.fail;
import static fj.data.Validation.success;

@data
public abstract class Person {

  public abstract <R> R match(Case<R> cases);

  public Validation<String, Person> changeName(PersonName newName) {
    return getDateOfDecease(this).isSome()
        ? fail("Cannot change a dead person name")
        : success(setPersonName(newName).f(this));
  }

  public interface Case<R> {
    R Person(PersonName personName, Contact contact, Option<LocalDate> dateOfDecease);
  }

  static final Equal<LocalDate> localDateEqual = Equal.anyEqual();
  static final Hash<LocalDate> localDateHash = Hash.anyHash();
  static final Show<LocalDate> localDateShow = Show.anyShow();
  static final Ord<LocalDate> localDateOrd = Ord.<ChronoLocalDate>comparableOrd().contramap(d -> d);

}
