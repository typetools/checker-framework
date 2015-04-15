package java.io;

import org.checkerframework.checker.nullness.qual.Nullable;


public abstract interface ObjectInputValidation{
  public abstract void validateObject() throws InvalidObjectException;
}
