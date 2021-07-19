import org.checkerframework.checker.interning.qual.*;

public class UnboxUninterned {
  void negation() {
    Boolean t = Boolean.valueOf(true);
    boolean b1 = !t.booleanValue();
    boolean b2 = !t;

    Integer x = Integer.valueOf(222222);
    int i1 = -x.intValue();
    int i2 = -x;
  }
}
