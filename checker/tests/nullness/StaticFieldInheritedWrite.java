import org.checkerframework.checker.nullness.qual.Nullable;

// A static field is the same location regardless of the class through which it is accessed, so
// writing it through one subclass invalidates refinements of it through an unrelated subclass.
public class StaticFieldInheritedWrite {

  static class Base {
    static @Nullable Object f;
  }

  static class Left extends Base {}

  static class Right extends Base {}

  void test() {
    Right.f = new Object();
    Right.f.toString();
    Left.f = null;
    // :: error: (dereference.of.nullable)
    Right.f.toString();
  }
}
