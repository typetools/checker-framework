import org.checkerframework.framework.test.*;
import org.checkerframework.framework.testchecker.util.*;

public class NonMethodCode {

  @Odd String f1 = null;
  String g1 = "def";

  static @Odd String sf1 = null;
  static String sg1 = "def";

  // test flow for field initializer
  @Odd String f2 = g1 == f1 ? g1 : f1;

  // test flow for initializer blocks
  {
    String l1 = f1;
    @Odd String l2 = l1;
    if (g1 == f1) {
      @Odd String l3 = g1;
    }
    // :: error: (assignment.type.incompatible)
    @Odd String l4 = g1;
  }

  // test flow for static initializer blocks
  static {
    String l1 = sf1;
    @Odd String l2 = l1;
    if (sg1 == sf1) {
      @Odd String l3 = sg1;
    }
    // :: error: (assignment.type.incompatible)
    @Odd String l4 = sg1;
  }
}
