package java.lang;

import checkers.nullness.quals.*;

@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public abstract @interface SuppressWarnings{
  public abstract java.lang.String[] value();
}
