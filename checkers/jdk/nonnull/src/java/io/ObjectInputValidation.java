package java.io;

import checkers.nonnull.quals.Nullable;


public abstract interface ObjectInputValidation{
  public abstract void validateObject() throws InvalidObjectException;
}
