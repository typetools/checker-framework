package lib;

/** Hands out a {@link SuperTypeArg}, like Beam's {@code DatastoreIO.v1().read()}. */
public class Factory {
  public static SuperTypeArg make() {
    return new SuperTypeArg();
  }
}
