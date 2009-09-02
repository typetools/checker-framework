package java.util;
import checkers.nullness.quals.*;
@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public interface Formattable{
  public abstract void formatTo(java.util.Formatter a1, int a2, int a3, int a4);
}
