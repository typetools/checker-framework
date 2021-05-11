import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.framework.test.*;
import org.checkerframework.framework.testchecker.util.*;

// Tests for the meta-annotations for contracts.
public class MetaPrecondition {

  String f1, f2, f3;
  MetaPrecondition p;

  @RequiresOdd("f1")
  void requiresF1() {
    // :: error: (assignment)
    @Value String l1 = f1;
    @Odd String l2 = f1;
  }

  @Pure
  @RequiresOdd("f1")
  int requiresF1AndPure() {
    // :: error: (assignment)
    @Value String l1 = f1;
    @Odd String l2 = f1;
    return 1;
  }

  @RequiresOdd("---")
  // :: error: (flowexpr.parse.error)
  void error() {
    // :: error: (assignment)
    @Value String l1 = f1;
    // :: error: (assignment)
    @Odd String l2 = f1;
  }

  @RequiresOdd("#1")
  void requiresParam(String p) {
    // :: error: (assignment)
    @Value String l1 = p;
    @Odd String l2 = p;
  }

  @RequiresOdd({"#1", "#2"})
  void requiresParams(String p1, String p2) {
    // :: error: (assignment)
    @Value String l1 = p1;
    // :: error: (assignment)
    @Value String l2 = p2;
    @Odd String l3 = p1;
    @Odd String l4 = p2;
  }

  @RequiresOdd("#1")
  // :: error: (flowexpr.parse.index.too.big)
  void param3() {}

  void t1(@Odd String p1, String p2) {
    // :: error: (contracts.precondition)
    requiresF1();
    // :: error: (contracts.precondition)
    requiresParam(p2);
    // :: error: (contracts.precondition)
    requiresParams(p1, p2);
  }

  void t2(@Odd String p1, String p2) {
    f1 = p1;
    requiresF1();
    // :: error: (contracts.precondition)
    requiresF1();
  }

  void t3(@Odd String p1, String p2) {
    f1 = p1;
    requiresF1AndPure();
    requiresF1AndPure();
    requiresF1AndPure();
    requiresF1();
    // :: error: (contracts.precondition)
    requiresF1();
  }
}
