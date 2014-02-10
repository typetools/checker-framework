package java.util;
import checkers.nullness.quals.Nullable;

public interface Observer {
  public abstract void update(Observable a1, Object a2);
}
