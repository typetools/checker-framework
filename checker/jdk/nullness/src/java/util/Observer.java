package java.util;
import org.checkerframework.checker.nullness.qual.Nullable;

public interface Observer {
  public abstract void update(Observable a1, Object a2);
}
