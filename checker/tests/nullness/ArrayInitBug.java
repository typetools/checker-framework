import org.checkerframework.checker.nullness.qual.*;

public class ArrayInitBug {

  @Nullable Object @Nullable [] aa;

  public ArrayInitBug() {
    aa = null;
  }
}
