package java.util;
import checkers.nullness.quals.*;
@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public interface Enumeration<E extends @Nullable Object> {
  public abstract boolean hasMoreElements();
  public abstract E nextElement();
}
