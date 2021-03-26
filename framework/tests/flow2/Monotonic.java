import org.checkerframework.framework.test.*;
import org.checkerframework.framework.testchecker.util.*;

public class Monotonic {

  String f1;
  @MonotonicOdd String f2;
  @MonotonicOdd String f2b;
  @Odd String f3;
  Monotonic[] ms;

  void nonpure() {}

  void t1(@Odd String p1) {
    // :: error: (assignment.type.incompatible)
    @Odd String l1 = f2;
    if (f2 == p1) {
      @Odd String l2 = f2;
      nonpure();
      @Odd String l3 = f2;
    }
  }

  void t2(@Odd String p1) {
    // :: error: (assignment.type.incompatible)
    f2 = f1;
    // :: error: (monotonic.type.incompatible)
    f2 = f2b; // assigning @MonotonicOdd to @MonotonicOdd is not allowed
  }

  void t3(@Odd String p1) {
    // :: error: (assignment.type.incompatible)
    @Odd String l1 = f2;
    f2 = p1;
    @Odd String l2 = f2;
    nonpure();
    @Odd String l3 = f2;
  }

  void t4(@Odd String p1) {
    // :: error: (assignment.type.incompatible)
    @Odd String l1 = f2;
    f2 = p1;
    @Odd String l2 = f2;
    ms[0].f2 = p1;
    @Odd String l3 = f2;
  }
}
