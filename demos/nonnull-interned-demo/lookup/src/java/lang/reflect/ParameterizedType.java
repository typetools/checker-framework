package java.lang.reflect;

import checkers.quals.*;

public abstract interface ParameterizedType{
  public abstract
    @NonNull Type @NonNull [] getActualTypeArguments();
  public abstract @NonNull Type getRawType();
  public abstract Type getOwnerType();
}
