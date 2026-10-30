import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.framework.testchecker.util.*;

// The desugared close() call of a try-with-resources statement is the close() method that the
// resource's class inherits, not an abstract close() method of an interface.
//
// The inherited close() method is side-effect-free, so a refinement made before the statement still
// holds after it.  Any other close() method may have side effects, which would discard the
// refinement.  (A postcondition of close() would not test this, because the statement does not
// call close() if the resource is null.)
public class TryWithResourcesClose {
  static @Odd String staticOdd;
  static String staticF;

  static class Base {
    @Odd String odd;
    String f;

    @SideEffectFree
    public void close() {}
  }

  interface MyCloseable extends AutoCloseable {
    @Override
    void close() throws java.io.IOException;
  }

  static class Resource extends Base implements MyCloseable {}

  void m(Resource r) {
    r.f = r.odd;
    try (r) {}
    @Odd String s = r.f;
  }

  <T extends Base & MyCloseable> void intersection(T r) throws Exception {
    r.f = r.odd;
    try (r) {}
    @Odd String s = r.f;
  }

  interface DefaultClose extends MyCloseable {
    @Override
    @SideEffectFree
    default void close() {}
  }

  static class DefaultResource implements DefaultClose {}

  void defaultMethod(DefaultResource r) {
    staticF = staticOdd;
    try (r) {}
    @Odd String s = staticF;
  }

  void interfaceType(DefaultClose r) {
    staticF = staticOdd;
    try (r) {}
    @Odd String s = staticF;
  }

  // A private method is not inherited, so it is not the method that the statement calls.
  interface APrivateClose {
    private void close() {}
  }

  static class PrivateCloseResource extends Base implements APrivateClose, AutoCloseable {}

  void privateMethod(PrivateCloseResource r) {
    r.f = r.odd;
    try (r) {}
    @Odd String s = r.f;
  }

  static class PrivateCloseBase {
    private void close() {}
  }

  static class PrivateCloseSubclass extends PrivateCloseBase implements DefaultClose {}

  void privateSuperclassMethod(PrivateCloseSubclass r) {
    staticF = staticOdd;
    try (r) {}
    @Odd String s = staticF;
  }

  // The abstract close() method may have side effects.
  void abstractMethod(MyCloseable r) throws Exception {
    staticF = staticOdd;
    try (r) {}
    // :: error: [assignment]
    @Odd String s = staticF;
  }
}
