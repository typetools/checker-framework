package java.util;
import checkers.nullness.quals.*;
@checkers.quals.DefaultQualifier(checkers.nullness.quals.NonNull.class)

public interface Observer {
  public abstract void update(Observable a1, Object a2);
}
