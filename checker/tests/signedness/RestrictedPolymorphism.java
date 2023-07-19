import org.checkerframework.checker.signedness.qual.PolySigned;
import org.checkerframework.checker.signedness.qual.Signed;
import org.checkerframework.checker.signedness.qual.Unsigned;

public class RestrictedPolymorphism {

  @Signed Number sn;
  @Unsigned Number un;

  public void foo(@PolySigned Object a, @PolySigned Object b) {}

  void client() {
    foo(sn, sn);
    // :: error: (argument)
    foo(sn, un);
    // :: error: (argument)
    foo(un, sn);
    foo(un, un);
  }
}
