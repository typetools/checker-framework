package java.lang;

import checkers.nullness.quals.*;

@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public final @NonNull class Void {
  protected Void() {}
  public final static java.lang.Class<java.lang.Void> TYPE = Void.class;
}
