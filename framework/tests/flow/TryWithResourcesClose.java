import org.checkerframework.framework.qual.EnsuresQualifier;
import org.checkerframework.framework.testchecker.util.*;

// The desugared close() call of a try-with-resources statement is the close() method that the
// resource's class inherits, not an abstract close() method of an interface.
public class TryWithResourcesClose {
  static class Base {
    @Odd String odd;
    String f;

    @EnsuresQualifier(expression = "f", qualifier = Odd.class)
    public void close() {
      f = odd;
    }
  }

  interface MyCloseable extends AutoCloseable {
    @Override
    void close() throws java.io.IOException;
  }

  static class Resource extends Base implements MyCloseable {}

  void m(Resource r) {
    try (r) {}
    @Odd String s = r.f;
  }
}
