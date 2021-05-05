import org.checkerframework.dataflow.qual.Deterministic;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.framework.test.*;
import org.checkerframework.framework.testchecker.util.*;

// various tests about keeping information in the store about pure method calls
public class StorePure {

  String f1, f2;

  // various pure methods

  @Pure
  String pure1() {
    return null;
  }

  @Pure
  String pure1b() {
    return null;
  }

  @Deterministic
  String pure1c() {
    return null;
  }

  @Pure
  String pure2(int i) {
    return null;
  }

  @Pure
  String pure3(boolean b) {
    return null;
  }

  @Pure
  String pure4(String o) {
    return null;
  }

  void nonpure() {}

  void t1(@Odd String p1, String p2, boolean b1) {
    String l0 = "";
    if (pure1() == p1) {
      @Odd String l1 = pure1();
      l0 = "a"; // does not remove information
      @Odd String l1b = pure1();
      // :: error: (assignment)
      @Odd String l2 = pure1b();
      nonpure(); // non-pure method call might change the return value of pure1
      // :: error: (assignment)
      @Odd String l3 = pure1();
    }
  }

  // check that it only works for deterministic methods
  void t1b(@Odd String p1, String p2, boolean b1) {
    if (pure1c() == p1) {
      @Odd String l1 = pure1c();
    }
  }

  void t2(@Odd String p1, String p2, boolean b1) {
    String l0 = "";
    if (pure1() == p1) {
      @Odd String l1 = pure1();
      l0 = "a"; // does not remove information
      @Odd String l1b = pure1();
      // :: error: (assignment)
      @Odd String l2 = pure1b();
      f1 = ""; // field update might change the return value of pure1
      // :: error: (assignment)
      @Odd String l3 = pure1();
    }
  }

  void t3(@Odd String p1, String p2, boolean b1) {
    String l0 = "";
    if (pure2(1) == p1) {
      // :: error: (assignment)
      @Odd String l4 = pure2(0);
      @Odd String l1 = pure2(1);
      l0 = "a"; // does not remove information
      @Odd String l1b = pure2(1);
      nonpure(); // non-pure method call might change the return value of pure2
      // :: error: (assignment)
      @Odd String l3 = pure2(1);
    }
  }

  void t4(@Odd String p1, String p2, boolean b1) {
    String l0 = "";
    if (pure2(1) == p1) {
      // :: error: (assignment)
      @Odd String l4 = pure2(0);
      @Odd String l1 = pure2(1);
      l0 = "a"; // does not remove information
      @Odd String l1b = pure2(1);
      f1 = ""; // field update might change the return value of pure2
      // :: error: (assignment)
      @Odd String l3 = pure2(1);
    }
  }

  void t5(@Odd String p1, String p2, boolean b1) {
    String l0 = "";
    if (pure3(true) == p1) {
      // :: error: (assignment)
      @Odd String l4 = pure3(false);
      @Odd String l1 = pure3(true);
      l0 = "a"; // does not remove information
      @Odd String l1b = pure3(true);
      nonpure(); // non-pure method call might change the return value of pure2
      // :: error: (assignment)
      @Odd String l3 = pure3(true);
    }
  }

  void t6(@Odd String p1, String p2, boolean b1) {
    String l0 = "";
    if (pure3(true) == p1) {
      // :: error: (assignment)
      @Odd String l4 = pure3(false);
      @Odd String l1 = pure3(true);
      l0 = "a"; // does not remove information
      @Odd String l1b = pure3(true);
      f1 = ""; // field update might change the return value of pure2
      // :: error: (assignment)
      @Odd String l3 = pure3(true);
    }
  }

  // local variable as argument
  void t7(@Odd String p1, String p2, boolean b1) {
    String l0 = "";
    if (pure4(l0) == p1) {
      // :: error: (assignment)
      @Odd String l4 = pure4("jk");
      @Odd String l1 = pure4(l0);
      l0 = "a"; // remove information (!)
      // :: error: (assignment)
      @Odd String l1b = pure4(l0);
    }
  }
}
