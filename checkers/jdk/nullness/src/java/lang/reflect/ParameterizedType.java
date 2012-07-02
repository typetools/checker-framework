package java.lang.reflect;

import checkers.nullness.quals.NonNull;
import checkers.nullness.quals.Nullable;

public abstract interface ParameterizedType extends Type {
  public abstract
    @NonNull Type @NonNull [] getActualTypeArguments();
  public abstract @NonNull Type getRawType();
  public abstract Type getOwnerType();
}
