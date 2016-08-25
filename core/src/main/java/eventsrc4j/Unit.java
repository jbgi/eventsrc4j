package eventsrc4j;

public final class Unit {

  private static final Unit u = new Unit();

  private Unit() {
  }

  public static Unit unit() {
    return u;
  }
}
