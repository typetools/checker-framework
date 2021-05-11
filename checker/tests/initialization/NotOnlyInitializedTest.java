import org.checkerframework.checker.initialization.qual.NotOnlyInitialized;
import org.checkerframework.checker.initialization.qual.UnderInitialization;

public class NotOnlyInitializedTest {

  @NotOnlyInitialized NotOnlyInitializedTest f;
  NotOnlyInitializedTest g;

  public NotOnlyInitializedTest() {
    f = new NotOnlyInitializedTest();
    g = new NotOnlyInitializedTest();
  }

  public NotOnlyInitializedTest(char i) {
    // we can store something that is under initialization (like this) in f, but not in g
    f = this;
    // :: error: (assignment)
    g = this;
  }

  static void testDeref(NotOnlyInitializedTest o) {
    // o is fully iniatlized, so we can dereference its fields
    o.f.toString();
    o.g.toString();
  }

  static void testDeref2(@UnderInitialization NotOnlyInitializedTest o) {
    // o is not fully iniatlized, so we cannot dereference its fields
    // :: error: (dereference.of.nullable)
    o.f.toString();
    // :: error: (dereference.of.nullable)
    o.g.toString();
  }
}
