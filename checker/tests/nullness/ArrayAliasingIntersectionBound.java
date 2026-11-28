import org.checkerframework.checker.nullness.qual.Nullable;

// Arrays whose element types are a type variable and one of its bounds can be the same array.
public class ArrayAliasingIntersectionBound {

  static <T extends Number & Comparable<T>> void writeThroughTypeVariable(
      @Nullable T[] ts, @Nullable Comparable<T>[] cs, T t) {
    if (cs[0] != null) {
      ts[0] = null;
      // If `ts` and `cs` are the same array, the assignment to `ts[0]` sets `cs[0]` to null.
      // :: error: [dereference.of.nullable]
      cs[0].compareTo(t);
    }
  }

  static <T extends Number & Comparable<T>> void writeThroughBound(
      @Nullable T[] ts, @Nullable Comparable<T>[] cs, T t) {
    if (ts[0] != null) {
      cs[0] = null;
      // If `ts` and `cs` are the same array, the assignment to `cs[0]` sets `ts[0]` to null.
      // :: error: [dereference.of.nullable]
      ts[0].intValue();
    }
  }

  static void unrelatedArrays(@Nullable Integer[] is, @Nullable String[] ss) {
    if (is[0] != null) {
      ss[0] = null;
      is[0].intValue();
    }
  }
}
