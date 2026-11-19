import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;

// Arrays whose element types are two parameterizations of a generic type can be the same array,
// even if neither parameterization is a subtype of the other.
public class ArrayAliasingParameterizations {

  static void elementOfWildcardLists(
      @Nullable List<? extends Number>[] xs, @Nullable List<? super Integer>[] ys) {
    if (xs[0] != null) {
      ys[0] = null;
      // If `xs` and `ys` are the same array, the assignment to `ys[0]` sets `xs[0]` to null.
      // :: error: [dereference.of.nullable]
      xs[0].size();
    }
  }
}
