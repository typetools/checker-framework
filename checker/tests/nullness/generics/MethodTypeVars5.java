import org.checkerframework.checker.nullness.qual.*;

public class MethodTypeVars5 {
  class B<S extends @Nullable Object> {
    S t;

    B(S t) {
      this.t = t;
    }

    S get() {
      return t;
    }
  }

  B<String> b = new B<>("Hello World");

  String doit1() {
    return doit1(b);
  }

  <U extends @Nullable Object> U doit1(B<U> x) {
    return x.get();
  }

  String doit2() {
    // Passing the null argument has no effect on the inferred type argument:
    // the second parameter type doesn't contain the type variable at all.
    return doit2(b, null);
  }

  <T extends @Nullable Object> T doit2(B<T> x, @Nullable String y) {
    return x.get();
  }

  String doit3() {
    // Passing the null argument has no effect on the inferred type argument:
    // the type variable only appears as nested type.
    return doit3(null);
  }

  String doit3b() {
    return doit3(new B<String>("Hi"));
  }

  String doit3b2() {
    return doit3(new B<>("Hi"));
  }

  String doit3c() {
    // :: error: (return.type.incompatible)
    return doit3(new B<@Nullable String>("Hi"));
  }

  void doit3d() {
    // :: error: (assignment.type.incompatible)
    @NonNull String s = doit3(new B<@Nullable String>("Hi"));
  }

  void doit3e() {
    String s = doit3(new B<String>("Hi"));
  }

  void doit3e2() {
    String s = doit3(new B<>("Hi"));
  }

  <T extends @Nullable Object> T doit3(@Nullable B<T> x) {
    if (x != null) {
      return x.get();
    } else {
      // This won't work at runtime, but whatever.
      @SuppressWarnings("unchecked")
      T res = (T) new Object();
      return res;
    }
  }

  String doit4() {
    // Passing the null argument has an impact on the inferred type argument:
    // the type variable appears as the top-level type.
    // :: error: (return.type.incompatible)
    return doit4("Ha!", null);
  }

  <T extends @Nullable Object> T doit4(T x, T y) {
    return x;
  }
}
