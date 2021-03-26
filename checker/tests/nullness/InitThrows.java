import org.checkerframework.checker.nullness.qual.*;

public class InitThrows {
  private final Object o;

  {
    try {
      o = new Object();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
