import org.checkerframework.checker.nullness.qual.*;

public class Simple2 {

  @NonNull String f;

  public Simple2() {
    // :: error: (method.invocation)
    test();

    f = "abc";
  }

  public void test() {
    System.out.println(f.toLowerCase());
  }

  public void a(Simple2 arg) {
    @Nullable String s = null;
    // :: error: (dereference.of.nullable)
    s.hashCode();
  }

  public static void main(String[] args) {
    new Simple2();
  }
}
