import org.checkerframework.checker.nullness.qual.EnsuresNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public abstract class AbstractClass {
  @Nullable Object f;

  @EnsuresNonNull("f")
  public abstract void setf();

  public abstract void setg();
}
