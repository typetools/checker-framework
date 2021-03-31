import org.checkerframework.checker.nullness.qual.Nullable;

public class Issue3443 {
  static <T extends Supplier3443<@Nullable String>> Supplier3443<String> passThrough(T t) {
    // :: error: (return.type.incompatible)
    return t;
  }

  public static void main(String[] args) {
    Supplier3443<@Nullable String> s1 = () -> null;
    // TODO: passThrough(s1) should cause an error. #979.
    Supplier3443<String> s2 = passThrough(s1);
    s2.get().toString();
  }
}

interface Supplier3443<T> {
  T get();
}
