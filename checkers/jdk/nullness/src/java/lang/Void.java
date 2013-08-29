package java.lang;

import checkers.nullness.quals.*;

@checkers.quals.DefaultQualifier(checkers.nullness.quals.NonNull.class)

public final class Void {
  protected Void() {}
  public final static Class<Void> TYPE = Void.class;
}
