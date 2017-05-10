package eventsrc4j;

import fj.Equal;
import fj.Hash;
import fj.Ord;
import fj.Show;
import java.time.Instant;

public final class TimeInstances {

  private TimeInstances(){}

  public static final Equal<Instant> instantEqual = Equal.anyEqual();

  public static final Hash<Instant> instantHash = Hash.anyHash();

  public static final Show<Instant> instantShow = Show.anyShow();

  public static final Ord<Instant> instantOrd = Ord.comparableOrd();
}
