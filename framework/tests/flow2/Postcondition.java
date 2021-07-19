import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.framework.qual.EnsuresQualifier;
import org.checkerframework.framework.qual.EnsuresQualifierIf;
import org.checkerframework.framework.test.*;
import org.checkerframework.framework.testchecker.util.*;

public class Postcondition {

  String f1, f2, f3;
  Postcondition p;

  @Pure
  String p1() {
    return null;
  }

  /** *** normal postcondition ***** */
  @EnsuresQualifier(expression = "f1", qualifier = Odd.class)
  void oddF1() {
    f1 = null;
  }

  @EnsuresQualifier(expression = "p.f1", qualifier = Odd.class)
  void oddF1_1() {
    p.f1 = null;
  }

  @EnsuresQualifier(expression = "#1.f1", qualifier = Odd.class)
  void oddF1_2(final Postcondition param) {
    param.f1 = null;
  }

  @EnsuresQualifier(expression = "p.p1()", qualifier = Odd.class)
  void oddF1_3() {
    if (p.p1() == null) {
      return;
    }
    throw new RuntimeException();
  }

  @EnsuresQualifier(expression = "f1", qualifier = Value.class)
  // :: error: (contracts.postcondition.not.satisfied)
  void valueF1() {}

  @EnsuresQualifier(expression = "---", qualifier = Value.class)
  // :: error: (flowexpr.parse.error)
  void error() {}

  @EnsuresQualifier(expression = "#1.#2", qualifier = Value.class)
  // :: error: (flowexpr.parse.error)
  void error2(final String p1, final String p2) {}

  @EnsuresQualifier(expression = "f1", qualifier = Value.class)
  void exception() {
    throw new RuntimeException();
  }

  @EnsuresQualifier(expression = "#1", qualifier = Value.class)
  void param1(final @Value String f) {}

  @EnsuresQualifier(
      expression = {"#1", "#2"},
      qualifier = Value.class)
  // :: error: (flowexpr.parameter.not.final)
  void param2(@Value String f, @Value String g) {
    f = g;
  }

  @EnsuresQualifier(expression = "#1", qualifier = Value.class)
  // :: error: (flowexpr.parse.index.too.big)
  void param3() {}

  // basic postcondition test
  void t1(@Odd String p1, String p2) {
    valueF1();
    // :: error: (assignment.type.incompatible)
    @Odd String l1 = f1;
    oddF1();
    @Odd String l2 = f1;

    // :: error: (flowexpr.parse.error.postcondition)
    error();
  }

  // test parameter syntax
  void t2(@Odd String p1, String p2) {
    // :: error: (flowexpr.parse.index.too.big)
    param3();
  }

  // postcondition with more complex expression
  void tn1(boolean b) {
    // :: error: (assignment.type.incompatible)
    @Odd String l1 = p.f1;
    oddF1_1();
    @Odd String l2 = p.f1;
  }

  // postcondition with more complex expression
  void tn2(boolean b) {
    Postcondition param = null;
    // :: error: (assignment.type.incompatible)
    @Odd String l1 = param.f1;
    oddF1_2(param);
    @Odd String l2 = param.f1;
  }

  // postcondition with more complex expression
  void tn3(boolean b) {
    // :: error: (assignment.type.incompatible)
    @Odd String l1 = p.p1();
    oddF1_3();
    @Odd String l2 = p.p1();
  }

  /** *** many postcondition ***** */
  @EnsuresQualifier.List({
    @EnsuresQualifier(expression = "f1", qualifier = Odd.class),
    @EnsuresQualifier(expression = "f2", qualifier = Value.class)
  })
  void oddValueF1(@Value String p1) {
    f1 = null;
    f2 = p1;
  }

  @EnsuresQualifier(expression = "f1", qualifier = Odd.class)
  @EnsuresQualifier(expression = "f2", qualifier = Value.class)
  void oddValueF1_repeated1(@Value String p1) {
    f1 = null;
    f2 = p1;
  }

  @EnsuresQualifier.List({
    @EnsuresQualifier(expression = "f1", qualifier = Odd.class),
  })
  @EnsuresQualifier(expression = "f2", qualifier = Value.class)
  void oddValueF1_repeated2(@Value String p1) {
    f1 = null;
    f2 = p1;
  }

  @EnsuresQualifier(expression = "f1", qualifier = Odd.class)
  @EnsuresQualifier.List({@EnsuresQualifier(expression = "f2", qualifier = Value.class)})
  void oddValueF1_repeated3(@Value String p1) {
    f1 = null;
    f2 = p1;
  }

  @EnsuresQualifier.List({
    @EnsuresQualifier(expression = "f1", qualifier = Odd.class),
    @EnsuresQualifier(expression = "f2", qualifier = Value.class)
  })
  // :: error: (contracts.postcondition.not.satisfied)
  void oddValueF1_invalid(@Value String p1) {}

  @EnsuresQualifier.List({
    @EnsuresQualifier(expression = "--", qualifier = Odd.class),
  })
  // :: error: (flowexpr.parse.error)
  void error2() {}

  // basic postcondition test
  void tnm1(@Odd String p1, @Value String p2) {
    // :: error: (assignment.type.incompatible)
    @Odd String l1 = f1;
    // :: error: (assignment.type.incompatible)
    @Value String l2 = f2;
    oddValueF1(p2);
    @Odd String l3 = f1;
    @Value String l4 = f2;

    // :: error: (flowexpr.parse.error.postcondition)
    error2();
  }

  /** *** conditional postcondition ***** */
  @EnsuresQualifierIf(result = true, expression = "f1", qualifier = Odd.class)
  boolean condOddF1(boolean b) {
    if (b) {
      f1 = null;
      return true;
    }
    return false;
  }

  @EnsuresQualifierIf(result = false, expression = "f1", qualifier = Odd.class)
  boolean condOddF1False(boolean b) {
    if (b) {
      return true;
    }
    f1 = null;
    return false;
  }

  @EnsuresQualifierIf(result = false, expression = "f1", qualifier = Odd.class)
  boolean condOddF1Invalid(boolean b) {
    if (b) {
      f1 = null;
      return true;
    }
    // :: error: (contracts.conditional.postcondition.not.satisfied)
    return false;
  }

  @EnsuresQualifierIf(result = false, expression = "f1", qualifier = Odd.class)
  // :: error: (contracts.conditional.postcondition.invalid.returntype)
  void wrongReturnType() {}

  @EnsuresQualifierIf(result = false, expression = "f1", qualifier = Odd.class)
  // :: error: (contracts.conditional.postcondition.invalid.returntype)
  String wrongReturnType2() {
    f1 = null;
    return "";
  }

  @EnsuresQualifierIf(result = true, expression = "#1", qualifier = Odd.class)
  boolean isOdd(final String p1) {
    return isOdd(p1, 0);
  }

  @EnsuresQualifierIf(result = true, expression = "#1", qualifier = Odd.class)
  boolean isOdd(final String p1, int p2) {
    return p1 == null;
  }

  @EnsuresQualifierIf(result = false, expression = "#1", qualifier = Odd.class)
  boolean isNotOdd(final String p1) {
    return !isOdd(p1);
  }

  // basic conditional postcondition test
  void t3(@Odd String p1, String p2) {
    condOddF1(true);
    // :: error: (assignment.type.incompatible)
    @Odd String l1 = f1;
    if (condOddF1(false)) {
      @Odd String l2 = f1;
    }
    // :: error: (assignment.type.incompatible)
    @Odd String l3 = f1;
  }

  // basic conditional postcondition test (inverted)
  void t4(@Odd String p1, String p2) {
    condOddF1False(true);
    // :: error: (assignment.type.incompatible)
    @Odd String l1 = f1;
    if (!condOddF1False(false)) {
      @Odd String l2 = f1;
    }
    // :: error: (assignment.type.incompatible)
    @Odd String l3 = f1;
  }

  // basic conditional postcondition test 2
  void t5(boolean b) {
    condOddF1(true);
    if (b) {
      // :: error: (assignment.type.incompatible)
      @Odd String l2 = f1;
    }
  }

  /** *** many conditional postcondition ***** */
  @EnsuresQualifierIf.List({
    @EnsuresQualifierIf(result = true, expression = "f1", qualifier = Odd.class),
    @EnsuresQualifierIf(result = false, expression = "f1", qualifier = Value.class)
  })
  boolean condsOddF1(boolean b, @Value String p1) {
    if (b) {
      f1 = null;
      return true;
    }
    f1 = p1;
    return false;
  }

  @EnsuresQualifierIf.List({
    @EnsuresQualifierIf(result = true, expression = "f1", qualifier = Odd.class),
    @EnsuresQualifierIf(result = false, expression = "f1", qualifier = Value.class)
  })
  boolean condsOddF1_invalid(boolean b, @Value String p1) {
    if (b) {
      // :: error: (contracts.conditional.postcondition.not.satisfied)
      return true;
    }
    // :: error: (contracts.conditional.postcondition.not.satisfied)
    return false;
  }

  @EnsuresQualifierIf.List({
    @EnsuresQualifierIf(result = false, expression = "f1", qualifier = Odd.class)
  })
  // :: error: (contracts.conditional.postcondition.invalid.returntype)
  String wrongReturnType3() {
    return "";
  }

  void t6(@Odd String p1, @Value String p2) {
    condsOddF1(true, p2);
    // :: error: (assignment.type.incompatible)
    @Odd String l1 = f1;
    // :: error: (assignment.type.incompatible)
    @Value String l2 = f1;
    if (condsOddF1(false, p2)) {
      @Odd String l3 = f1;
      // :: error: (assignment.type.incompatible)
      @Value String l4 = f1;
    } else {
      @Value String l5 = f1;
      // :: error: (assignment.type.incompatible)
      @Odd String l6 = f1;
    }
  }
}
