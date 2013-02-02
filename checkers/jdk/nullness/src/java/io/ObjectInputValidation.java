package java.io;

import checkers.nullness.quals.*;

@checkers.quals.DefaultQualifier(checkers.nullness.quals.NonNull.class)

public abstract interface ObjectInputValidation{
  public abstract void validateObject() throws InvalidObjectException;
}
