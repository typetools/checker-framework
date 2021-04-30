import org.checkerframework.checker.nullness.qual.*;

public class Aliasing {
  @NonNull Object nno = new Object();
  @Nullable Object no = null;

  public static void main(String[] args) {
    Aliasing a = new Aliasing();
    Aliasing b = new Aliasing();
    m(a, b);
  }

  static void m(@NonNull Aliasing a, @NonNull Aliasing b) {
    a.no = b.nno;
    // Changing a.no to nonnull does not mean that b.no is also nonnull
    // :: error: (assignment)
    b.nno = b.no;

    System.out.println("@NonNull field b.nno is: " + b.nno);
  }
}
