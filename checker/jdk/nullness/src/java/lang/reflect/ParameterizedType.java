package java.lang.reflect;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public interface ParameterizedType extends Type {
  public abstract
    @NonNull Type @NonNull [] getActualTypeArguments();
  public abstract @NonNull Type getRawType();
  public abstract Type getOwnerType();
}
