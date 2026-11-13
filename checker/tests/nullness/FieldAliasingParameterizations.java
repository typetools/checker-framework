import org.checkerframework.checker.nullness.qual.Nullable;

// Receivers whose types are two parameterizations of a generic type can be the same object, even if
// neither parameterization is a subtype of the other.
public class FieldAliasingParameterizations {

  static class Box<T> {
    @Nullable String f;
  }

  static void fieldOfWildcardBoxes(Box<? extends Number> x, Box<? super Integer> y) {
    if (x.f != null) {
      y.f = null;
      // If `x` and `y` are the same object, the assignment to `y.f` sets `x.f` to null.
      // :: error: [dereference.of.nullable]
      x.f.length();
    }
  }
}
