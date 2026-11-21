import org.checkerframework.checker.nullness.qual.Nullable;

// A multi-catch exception parameter can be the same object as an expression whose type is related
// to one of the alternatives, but not as an expression whose type is unrelated to every
// alternative.
public class FieldAliasingUnionReceiver {

  static class Base extends Exception {
    @Nullable String f;
  }

  static class E1 extends Base {}

  static class E2 extends Base {}

  static class E3 extends Base {}

  static void throwSomething(boolean b) throws E1, E2 {
    if (b) {
      throw new E1();
    } else {
      throw new E2();
    }
  }

  static void relatedToAlternative(E1 x, boolean b) {
    try {
      throwSomething(b);
    } catch (E1 | E2 e) {
      if (x.f != null) {
        e.f = null;
        // If `e` and `x` are the same object, the assignment to `e.f` sets `x.f` to null.
        // :: error: [dereference.of.nullable]
        x.f.length();
      }
    }
  }

  static void unrelatedToEveryAlternative(E3 x, boolean b) {
    try {
      throwSomething(b);
    } catch (E1 | E2 e) {
      if (x.f != null) {
        e.f = null;
        // `e` is an `E1` or an `E2`, so it is not the same object as `x`, which is an `E3`.
        x.f.length();
      }
    }
  }
}
