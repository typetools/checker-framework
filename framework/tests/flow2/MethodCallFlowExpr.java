import org.checkerframework.dataflow.qual.*;
import org.checkerframework.framework.qual.EnsuresQualifier;
import org.checkerframework.framework.testchecker.util.*;

public class MethodCallFlowExpr {

  @Pure
  String p1(int i) {
    return "";
  }

  @Pure
  String p1b(Long i) {
    return "";
  }

  @Pure
  String p1(String s) {
    return "";
  }

  String nonpure() {
    return "";
  }

  @Pure
  String p2(String s, long l, String s2) {
    return "";
  }

  @Pure
  <T> String p1c(T i) {
    return "";
  }

  @Pure
  static String p1d(int i) {
    return "";
  }

  @EnsuresQualifier(expression = "p1c(\"abc\")", qualifier = Odd.class)
  // :: error: (contracts.postcondition.not.satisfied)
  void e0() {
    // don't bother with implementation
  }

  @EnsuresQualifier(expression = "p1(1)", qualifier = Odd.class)
  // :: error: (contracts.postcondition.not.satisfied)
  void e1() {
    // don't bother with implementation
  }

  @EnsuresQualifier(expression = "p1(\"abc\")", qualifier = Odd.class)
  // :: error: (contracts.postcondition.not.satisfied)
  void e2() {
    // don't bother with implementation
  }

  @EnsuresQualifier(expression = "p2(\"abc\", 2L, p1(1))", qualifier = Odd.class)
  // :: error: (contracts.postcondition.not.satisfied)
  void e4() {
    // don't bother with implementation
  }

  @EnsuresQualifier(expression = "p1b(2L)", qualifier = Odd.class)
  // :: error: (contracts.postcondition.not.satisfied)
  void e5() {
    // don't bother with implementation
  }

  @EnsuresQualifier(expression = "p1b(null)", qualifier = Odd.class)
  // :: error: (contracts.postcondition.not.satisfied)
  void e6() {
    // don't bother with implementation
  }

  @EnsuresQualifier(expression = "p1d(1)", qualifier = Odd.class)
  // :: error: (contracts.postcondition.not.satisfied)
  void e7a() {
    // don't bother with implementation
  }

  @EnsuresQualifier(expression = "this.p1d(1)", qualifier = Odd.class)
  // :: error: (contracts.postcondition.not.satisfied)
  void e7b() {
    // don't bother with implementation
  }

  @EnsuresQualifier(expression = "MethodCallFlowExpr.p1d(1)", qualifier = Odd.class)
  // :: error: (contracts.postcondition.not.satisfied)
  void e7c() {
    // don't bother with implementation
  }

  void t1() {
    // :: error: (assignment.type.incompatible)
    @Odd String l1 = p1(1);
    e1();
    // :: error: (assignment.type.incompatible)
    @Odd String l2 = p1(2);
    @Odd String l3 = p1(1);
  }

  void t2() {
    // :: error: (assignment.type.incompatible)
    @Odd String l1 = p1("abc");
    e2();
    // :: error: (assignment.type.incompatible)
    @Odd String l2 = p1("def");
    // :: error: (assignment.type.incompatible)
    @Odd String l2b = p1(1);
    @Odd String l3 = p1("abc");
  }

  void t3() {
    // :: error: (assignment.type.incompatible)
    @Odd String l1 = p2("abc", 2L, p1(1));
    e4();
    // :: error: (assignment.type.incompatible)
    @Odd String l2 = p2("abc", 2L, p1(2));
    // :: error: (assignment.type.incompatible)
    @Odd String l2b = p2("abc", 1L, p1(1));
    @Odd String l3 = p2("abc", 2L, p1(1));
  }

  void t4() {
    // :: error: (assignment.type.incompatible)
    @Odd String l1 = p1b(2L);
    e5();
    // :: error: (assignment.type.incompatible)
    @Odd String l2 = p1b(null);
    // :: error: (assignment.type.incompatible)
    @Odd String l2b = p1b(1L);
    // FIXME: the following line shouldn't give an error
    // (or at least it didn't give an error earlier)
    // @Odd String l3 = p1b(2L);
  }

  void t5() {
    // :: error: (assignment.type.incompatible)
    @Odd String l1 = p1b(null);
    e6();
    // :: error: (assignment.type.incompatible)
    @Odd String l2 = p1b(1L);
    // FIXME: the following line shouldn't give an error
    // (or at least it didn't give an error earlier)
    // @Odd String l3 = p1b(null);
  }

  void t6() {
    // :: error: (assignment.type.incompatible)
    @Odd String l1 = p1c("abc");
    e0();
    // :: error: (assignment.type.incompatible)
    @Odd String l2 = p1c("def");
    @Odd String l3 = p1c("abc");
  }

  void t7() {
    // :: error: (assignment.type.incompatible)
    @Odd String l1 = p1d(1);
    // :: error: (assignment.type.incompatible)
    @Odd String l1b = MethodCallFlowExpr.p1d(1);
    // :: error: (assignment.type.incompatible)
    @Odd String l1c = this.p1d(1);
    e7a();
    @Odd String l2 = p1d(1);
    @Odd String l2b = MethodCallFlowExpr.p1d(1);
    @Odd String l2c = this.p1d(1);
  }

  void t8() {
    // :: error: (assignment.type.incompatible)
    @Odd String l1 = p1d(1);
    // :: error: (assignment.type.incompatible)
    @Odd String l1b = MethodCallFlowExpr.p1d(1);
    // :: error: (assignment.type.incompatible)
    @Odd String l1c = this.p1d(1);
    e7b();
    @Odd String l2 = p1d(1);
    @Odd String l2b = MethodCallFlowExpr.p1d(1);
    @Odd String l2c = this.p1d(1);
  }

  void t9() {
    // :: error: (assignment.type.incompatible)
    @Odd String l1 = p1d(1);
    // :: error: (assignment.type.incompatible)
    @Odd String l1b = MethodCallFlowExpr.p1d(1);
    // :: error: (assignment.type.incompatible)
    @Odd String l1c = this.p1d(1);
    e7c();
    @Odd String l2 = p1d(1);
    @Odd String l2b = MethodCallFlowExpr.p1d(1);
    @Odd String l2c = this.p1d(1);
  }
}
