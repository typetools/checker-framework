import checkers.nullness.quals.*;

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
    b.nno = b.no;

    System.out.println("@NonNull field b.nno is: " + b.nno);
  }
}
