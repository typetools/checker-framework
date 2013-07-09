package java.io;

import checkers.nullness.quals.Nullable;


public abstract interface ObjectInputValidation{
  public abstract void validateObject() throws InvalidObjectException;
}
