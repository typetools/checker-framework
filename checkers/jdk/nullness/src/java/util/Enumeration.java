package java.util;
import checkers.nullness.quals.*;
@checkers.quals.DefaultQualifier(checkers.nullness.quals.NonNull.class)

public interface Enumeration<E extends @Nullable Object> {
  public abstract boolean hasMoreElements();
  public abstract E nextElement();
}
