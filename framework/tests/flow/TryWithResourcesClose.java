import org.checkerframework.framework.qual.EnsuresQualifier;
import org.checkerframework.framework.testchecker.util.*;

// The desugared close() call of a try-with-resources statement is the close() method that the
// resource's class inherits, not an abstract close() method of an interface.
public class TryWithResourcesClose {
  static @Odd String staticOdd;
  static String staticF;

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

  <T extends Base & MyCloseable> void intersection(T r) throws Exception {
    try (r) {}
    @Odd String s = r.f;
  }

  interface DefaultClose extends MyCloseable {
    @Override
    @EnsuresQualifier(expression = "TryWithResourcesClose.staticF", qualifier = Odd.class)
    default void close() {
      staticF = staticOdd;
    }
  }

  static class DefaultResource implements DefaultClose {}

  void defaultMethod(DefaultResource r) {
    try (r) {}
    @Odd String s = staticF;
  }

  void interfaceType(DefaultClose r) {
    try (r) {}
    @Odd String s = staticF;
  }

  // A private method is not inherited, so it is not the method that the statement calls.
  interface APrivateClose {
    private void close() {}
  }

  static class PrivateCloseResource extends Base implements APrivateClose, AutoCloseable {}

  void privateMethod(PrivateCloseResource r) {
    try (r) {}
    @Odd String s = r.f;
  }

  static class PrivateCloseBase {
    private void close() {}
  }

  static class PrivateCloseSubclass extends PrivateCloseBase implements DefaultClose {}

  void privateSuperclassMethod(PrivateCloseSubclass r) {
    try (r) {}
    @Odd String s = staticF;
  }
}
