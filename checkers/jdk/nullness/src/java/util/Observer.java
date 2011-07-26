package java.util;
import checkers.nullness.quals.*;
@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public interface Observer {
  public abstract void update(Observable a1, Object a2);
}
