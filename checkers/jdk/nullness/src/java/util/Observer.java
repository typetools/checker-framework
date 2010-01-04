package java.util;
import checkers.nullness.quals.*;
@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public interface Observer{
  public abstract void update(java.util.Observable a1, java.lang.Object a2);
}
