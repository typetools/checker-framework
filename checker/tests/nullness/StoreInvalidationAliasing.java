import org.checkerframework.checker.nullness.qual.Nullable;

// An assignment to a field or array element invalidates refinements of every expression that might
// denote the same location.
public class StoreInvalidationAliasing {

  static class Base {
    static @Nullable String f;
  }

  static class Left extends Base {}

  static class Right extends Base {}

  // `Left.f` and `Right.f` are the same static field, though `Left` and `Right` are unrelated.
  void inheritedStaticField() {
    if (((@Nullable String) Left.f) != null) {
      Right.f = null;
      // :: error: (dereference.of.nullable)
      Left.f.length();
    }
  }

  // `numbers` and `comparables` might be the same `Integer[]`.
  void arrayWriteThroughInterfaceAlias(
      @Nullable Number[] numbers, @Nullable Comparable<?>[] comparables) {
    if (((@Nullable Number) numbers[0]) != null) {
      comparables[0] = null;
      // :: error: (dereference.of.nullable)
      numbers[0].intValue();
    }
  }

  // `numbers` and `strings` cannot be the same array.
  void arrayWriteNoAlias(@Nullable Number[] numbers, @Nullable String[] strings) {
    if (numbers[0] != null) {
      strings[0] = null;
      numbers[0].intValue();
    }
  }
}
