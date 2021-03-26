import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class Issue2865<T extends @Nullable Object> {
  public class C {
    public C(T a) {}

    public void f(T a) {
      new C(a);
      // :: error: (argument.type.incompatible)
      new C(null);
    }
  }

  void test(Issue2865<@NonNull String> s) {
    // :: error: (argument.type.incompatible)
    s.new C(null);
    s.new C("");
  }

  void test2(Issue2865<@Nullable String> s) {
    s.new C(null);
    s.new C("");
  }
}
