import org.checkerframework.common.value.qual.*;

// @skip-test

public class MinLenVarargs {
  static void check(String @MinLen(3) ... var) {
    System.out.println(var[0] + " " + var[1]);
  }

  public static void main(String[] args) {
    // :: error: (argument)
    check(new String[] {"goodbye"});
    // :: error: (argument)
    check("goodbye");
    // :: error: (argument)
    check();
    // :: error: (argument)
    check("hello", "goodbye");
  }
}
