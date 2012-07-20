import checkers.quals.PolyAll;
import checkers.nullness.quals.*;

class TestPolyAll2 {
  // @PolyAll should apply to every type that has no explicit qualifier

  public static boolean noDuplicates1 (@PolyAll @NonNull String[] a) {
    // non-null
    a[0].hashCode();
    //:: error: (assignment.type.incompatible)
    a[0] = null;
    return true;
  }

  public static boolean noDuplicates2 (@PolyAll @Nullable String[] a) {
    // nullable
    a[0] = null;
    //:: error: (dereference.of.nullable)
    a[0].hashCode();
    return true;
  }

  // Ensure that ordering of qualifiers doesn't matter.
  public static boolean noDuplicates3 (@NonNull @PolyAll String[] a) {
    return false;
  }

  // Real duplicate forbidden.
  //:: error: (type.invalid)
  public static boolean noDuplicates4 (@NonNull @PolyAll @Nullable String[] a) {
    return true;
  }
}

