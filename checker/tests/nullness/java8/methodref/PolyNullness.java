import org.checkerframework.checker.nullness.qual.*;

interface PolyFunc {
  @PolyNull String method(@PolyNull String in);
}

interface NonNullFunc {
  @NonNull String method(@NonNull String in);
}

interface MixedFunc {
  @NonNull String method(@Nullable String in);
}

class Context {

  static @PolyNull String poly(@PolyNull String in) {
    return in;
  }

  static String nonPoly(String in) {
    return in;
  }

  void context() {
    PolyFunc f1 = Context::poly;
    // :: error: (methodref.param.invalid)
    PolyFunc f2 = Context::nonPoly;

    NonNullFunc f3 = Context::poly;
    // :: error: (methodref.return.invalid)
    MixedFunc f4 = Context::poly;
  }
}
