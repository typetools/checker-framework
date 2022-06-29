import java.util.Date;
import org.checkerframework.checker.signedness.qual.PolySigned;
import org.checkerframework.checker.signedness.qual.Signed;
import org.checkerframework.checker.signedness.qual.Unsigned;

public class RestrictedPolymorphism {

  @Signed Date sd;
  @Unsigned Date ud;

  public void foo(@PolySigned Object a, @PolySigned Object b) {}

  void client() {
    foo(sd, sd);
    // :: error: (argument)
    foo(sd, ud);
    // :: error: (argument)
    foo(ud, sd);
    foo(ud, ud);
  }
}
