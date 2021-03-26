import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.framework.qual.RequiresQualifier;
import org.checkerframework.framework.test.*;
import org.checkerframework.framework.testchecker.util.*;

// various tests for the precondition mechanism
public class Precondition {

  String f1, f2, f3;
  Precondition p;

  @RequiresQualifier(expression = "f1", qualifier = Odd.class)
  void requiresF1() {
    // :: error: (assignment.type.incompatible)
    @Value String l1 = f1;
    @Odd String l2 = f1;
  }

  @Pure
  @RequiresQualifier(expression = "f1", qualifier = Odd.class)
  int requiresF1AndPure() {
    // :: error: (assignment.type.incompatible)
    @Value String l1 = f1;
    @Odd String l2 = f1;
    return 1;
  }

  @RequiresQualifier(expression = "f1", qualifier = Value.class)
  void requiresF1Value() {
    // :: error: (assignment.type.incompatible)
    @Odd String l1 = f1;
    @Value String l2 = f1;
  }

  @RequiresQualifier(expression = "---", qualifier = Odd.class)
  // :: error: (flowexpr.parse.error)
  void error() {
    // :: error: (assignment.type.incompatible)
    @Value String l1 = f1;
    // :: error: (assignment.type.incompatible)
    @Odd String l2 = f1;
  }

  @RequiresQualifier(expression = "#1", qualifier = Odd.class)
  void requiresParam(String p) {
    // :: error: (assignment.type.incompatible)
    @Value String l1 = p;
    @Odd String l2 = p;
  }

  @RequiresQualifier(
      expression = {"#1", "#2"},
      qualifier = Odd.class)
  void requiresParams(String p1, String p2) {
    // :: error: (assignment.type.incompatible)
    @Value String l1 = p1;
    // :: error: (assignment.type.incompatible)
    @Value String l2 = p2;
    @Odd String l3 = p1;
    @Odd String l4 = p2;
  }

  @RequiresQualifier(expression = "#1", qualifier = Odd.class)
  // :: error: (flowexpr.parse.index.too.big)
  void param3() {}

  void t1(@Odd String p1, String p2) {
    // :: error: (contracts.precondition.not.satisfied)
    requiresF1();
    // :: error: (contracts.precondition.not.satisfied)
    requiresF1Value();
    // :: error: (contracts.precondition.not.satisfied)
    requiresParam(p2);
    // :: error: (contracts.precondition.not.satisfied)
    requiresParams(p1, p2);
  }

  void t2(@Odd String p1, String p2) {
    f1 = p1;
    requiresF1();
    // :: error: (contracts.precondition.not.satisfied)
    requiresF1();
    // :: error: (contracts.precondition.not.satisfied)
    requiresF1Value();
  }

  void t3(@Odd String p1, String p2) {
    f1 = p1;
    requiresF1AndPure();
    requiresF1AndPure();
    requiresF1AndPure();
    requiresF1();
    // :: error: (contracts.precondition.not.satisfied)
    requiresF1();
  }

  void t4(@Odd String p1, String p2, @Value String p3) {
    f1 = p1;
    requiresF1();
    f1 = p3;
    requiresF1Value();
    requiresParam(p1);
    requiresParams(p1, p1);
  }

  class Inner {
    @RequiresQualifier(expression = "f1", qualifier = Odd.class)
    void foo() {}
  }

  @Odd String f4;

  @RequiresQualifier(expression = "f4", qualifier = Odd.class)
  void requiresF4() {}

  void tt1() {
    requiresF4();
  }

  /** *** multiple preconditions ***** */
  @RequiresQualifier(expression = "f1", qualifier = Value.class)
  @RequiresQualifier(expression = "f2", qualifier = Odd.class)
  void multi() {
    @Value String l1 = f1;
    @Odd String l2 = f2;
    // :: error: (assignment.type.incompatible)
    @Value String l3 = f2;
    // :: error: (assignment.type.incompatible)
    @Odd String l4 = f1;
  }

  @RequiresQualifier.List({
    @RequiresQualifier(expression = "f1", qualifier = Value.class),
    @RequiresQualifier(expression = "f2", qualifier = Odd.class)
  })
  void multi_explicit_requiresqualifierlist() {
    @Value String l1 = f1;
    @Odd String l2 = f2;
    // :: error: (assignment.type.incompatible)
    @Value String l3 = f2;
    // :: error: (assignment.type.incompatible)
    @Odd String l4 = f1;
  }

  @RequiresQualifier.List({@RequiresQualifier(expression = "--", qualifier = Value.class)})
  // :: error: (flowexpr.parse.error)
  void error2() {}

  void t5(@Odd String p1, String p2, @Value String p3) {
    // :: error: (contracts.precondition.not.satisfied)
    multi();
    f1 = p3;
    // :: error: (contracts.precondition.not.satisfied)
    multi();
    f1 = p3;
    f2 = p1;
    multi();

    // :: error: (flowexpr.parse.error)
    error2();
  }
}
