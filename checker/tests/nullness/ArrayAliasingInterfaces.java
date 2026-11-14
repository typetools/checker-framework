import org.checkerframework.checker.nullness.qual.Nullable;

// Arrays whose element types are unrelated interfaces, or a class and an unrelated interface, can
// be the same array, because one object can implement both types.
public class ArrayAliasingInterfaces {

  interface I {}

  interface J {}

  static class C {}

  static final class FinalC {}

  static void twoInterfaces(@Nullable I[] is, @Nullable J[] js) {
    if (js[0] != null) {
      is[0] = null;
      // If `is` and `js` are the same array, the assignment to `is[0]` sets `js[0]` to null.
      // :: error: [dereference.of.nullable]
      js[0].toString();
    }
  }

  static void classAndInterface(@Nullable C[] cs, @Nullable Runnable[] rs) {
    if (cs[0] != null) {
      rs[0] = null;
      // If `cs` and `rs` are the same array, the assignment to `rs[0]` sets `cs[0]` to null.
      // :: error: [dereference.of.nullable]
      cs[0].toString();
    }
  }

  // No object is both a `FinalC` and an `I`, so `fs` and `is` cannot be the same array.
  static void finalClassAndInterface(@Nullable FinalC[] fs, @Nullable I[] is) {
    if (fs[0] != null) {
      is[0] = null;
      fs[0].toString();
    }
  }
}
