package java.io;

import checkers.nullness.quals.*;

@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public abstract interface ObjectInputValidation{
  public abstract void validateObject() throws InvalidObjectException;
}
