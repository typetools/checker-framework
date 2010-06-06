package java.util;
import checkers.nullness.quals.*;
@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public abstract class ListResourceBundle extends ResourceBundle {
  public ListResourceBundle() { throw new RuntimeException("skeleton method"); }
  public final @Nullable Object handleGetObject(@Nullable String a1) { throw new RuntimeException("skeleton method"); }
    public Enumeration<String> getKeys() { throw new RuntimeException("skeleton method"); }
}
