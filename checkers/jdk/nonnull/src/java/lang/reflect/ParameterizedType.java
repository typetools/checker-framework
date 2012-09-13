package java.lang.reflect;

import checkers.nonnull.quals.NonNull;
import checkers.nonnull.quals.Nullable;

public abstract interface ParameterizedType extends Type {
  public abstract
    @NonNull Type @NonNull [] getActualTypeArguments();
  public abstract @NonNull Type getRawType();
  public abstract Type getOwnerType();
}
