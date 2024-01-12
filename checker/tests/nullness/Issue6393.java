import java.io.IOException;
import java.io.Serializable;
import org.checkerframework.checker.nullness.qual.Nullable;

class Issue6393 {

  public static class AClass implements Serializable {}

  static class GClass {
    public <T extends Serializable> @Nullable T g(Class<? super T> e) throws IOException {
      throw new AssertionError();
    }
  }

  @SuppressWarnings("unchecked")
  static <T extends AClass> T f(Class<? super T> x, GClass y) {
    AClass z;
    try {
      z = y.g(x);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
    // :: warning: (cast.unsafe)
    return (T) z;
  }
}
