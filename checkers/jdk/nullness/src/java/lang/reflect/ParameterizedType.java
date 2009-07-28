package java.lang.reflect;

import checkers.nullness.quals.*;

public abstract interface ParameterizedType{
  public abstract
    @NonNull java.lang.reflect.Type @NonNull [] getActualTypeArguments();
  public abstract @NonNull java.lang.reflect.Type getRawType();
  public abstract java.lang.reflect.Type getOwnerType();
}
