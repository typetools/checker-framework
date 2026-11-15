import org.checkerframework.checker.nullness.qual.Nullable;

// An array whose element type is a type variable bounded by Object can be the same array as an
// array of any reference type, because a caller can instantiate the type variable to that type.
public class ArrayAliasingTypeVariables {

  static <T> void typeVariableAndClass(@Nullable T[] ts, @Nullable String[] ss) {
    if (ss[0] != null) {
      ts[0] = null;
      // If `ts` and `ss` are the same array, the assignment to `ts[0]` sets `ss[0]` to null.
      // :: error: [dereference.of.nullable]
      ss[0].length();
    }
  }

  static <T, S> void twoTypeVariables(@Nullable T[] ts, @Nullable S[] ss) {
    if (ss[0] != null) {
      ts[0] = null;
      // If `ts` and `ss` are the same array, the assignment to `ts[0]` sets `ss[0]` to null.
      // :: error: [dereference.of.nullable]
      ss[0].toString();
    }
  }
}
