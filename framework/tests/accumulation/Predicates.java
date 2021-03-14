import org.checkerframework.framework.testchecker.testaccumulation.qual.*;

public class Predicates {

  void testOr1() {
    MyClass m1 = new MyClass();

    // :: error: method.invocation.invalid
    m1.c();
  }

  void testOr2() {
    MyClass m1 = new MyClass();

    m1.a();
    m1.c();
  }

  void testOr3() {
    MyClass m1 = new MyClass();

    m1.b();
    m1.c();
  }

  void testAnd1() {
    MyClass m1 = new MyClass();

    // :: error: method.invocation.invalid
    m1.d();
  }

  void testAnd2() {
    MyClass m1 = new MyClass();

    m1.a();
    // :: error: method.invocation.invalid
    m1.d();
  }

  void testAnd3() {
    MyClass m1 = new MyClass();

    m1.b();
    // :: error: method.invocation.invalid
    m1.d();
  }

  void testAnd4() {
    MyClass m1 = new MyClass();

    m1.a();
    m1.c();
    // :: error: method.invocation.invalid
    m1.d();
  }

  void testAnd5() {
    MyClass m1 = new MyClass();

    m1.a();
    m1.b();
    m1.d();
  }

  void testAnd6() {
    MyClass m1 = new MyClass();

    m1.a();
    m1.b();
    m1.c();
    m1.d();
  }

  void testAndOr1() {
    MyClass m1 = new MyClass();

    // :: error: method.invocation.invalid
    m1.e();
  }

  void testAndOr2() {
    MyClass m1 = new MyClass();

    m1.a();
    m1.e();
  }

  void testAndOr3() {
    MyClass m1 = new MyClass();

    m1.b();
    // :: error: method.invocation.invalid
    m1.e();
  }

  void testAndOr4() {
    MyClass m1 = new MyClass();

    m1.b();
    m1.c();
    m1.e();
  }

  void testAndOr5() {
    MyClass m1 = new MyClass();

    m1.a();
    m1.b();
    m1.c();
    m1.d();
    m1.e();
  }

  void testPrecedence1() {
    MyClass m1 = new MyClass();

    // :: error: method.invocation.invalid
    m1.f();
  }

  void testPrecedence2() {
    MyClass m1 = new MyClass();

    m1.a();
    // :: error: method.invocation.invalid
    m1.f();
  }

  void testPrecedence3() {
    MyClass m1 = new MyClass();

    m1.b();
    // :: error: method.invocation.invalid
    m1.f();
  }

  void testPrecedence4() {
    MyClass m1 = new MyClass();

    m1.a();
    m1.b();
    m1.f();
  }

  void testPrecedence5() {
    MyClass m1 = new MyClass();

    m1.a();
    m1.c();
    m1.f();
  }

  void testPrecedence6() {
    MyClass m1 = new MyClass();

    m1.b();
    m1.c();
    m1.f();
  }

  private static class MyClass {

    @TestAccumulation("a") MyClass cmA;

    @TestAccumulationPredicate("a") MyClass cmpA;

    @TestAccumulation({"a", "b"}) MyClass aB;

    @TestAccumulationPredicate("a || b") MyClass aOrB;

    @TestAccumulationPredicate("a && b") MyClass aAndB;

    @TestAccumulationPredicate("a || b && c") MyClass bAndCOrA;

    @TestAccumulationPredicate("a || (b && c)") MyClass bAndCOrAParens;

    @TestAccumulationPredicate("a && b || c") MyClass aAndBOrC;

    @TestAccumulationPredicate("(a && b) || c") MyClass aAndBOrCParens;

    @TestAccumulationPredicate("(a || b) && c") MyClass aOrBAndC;

    @TestAccumulationPredicate("a && (b || c)") MyClass bOrCAndA;

    @TestAccumulationPredicate("b && c") MyClass bAndC;

    @TestAccumulationPredicate("(b && c)") MyClass bAndCParens;

    void a() {}

    void b() {}

    void c(@TestAccumulationPredicate("a || b") MyClass this) {}

    void d(@TestAccumulationPredicate("a && b") MyClass this) {}

    void e(@TestAccumulationPredicate("a || (b && c)") MyClass this) {}

    void f(@TestAccumulationPredicate("a && b || c") MyClass this) {}

    static void testAssignability1(@TestAccumulationPredicate("a || b") MyClass cAble) {
      cAble.c();
      // :: error: method.invocation.invalid
      cAble.d();
      // :: error: method.invocation.invalid
      cAble.e();
      // :: error: method.invocation.invalid
      cAble.f();
    }

    static void testAssignability2(@TestAccumulationPredicate("a && b") MyClass dAble) {
      // These calls would work if subtyping between predicates was by implication. They issue
      // errors, because it is not.
      // :: error: method.invocation.invalid
      dAble.c();
      dAble.d();
      // :: error: method.invocation.invalid
      dAble.e();
      // :: error: method.invocation.invalid
      dAble.f();
    }

    void testAllAssignability() {

      @TestAccumulation("a") MyClass cmALocal;
      @TestAccumulationPredicate("a") MyClass cmpALocal;
      @TestAccumulationPredicate("a || b") MyClass aOrBLocal;
      @TestAccumulation({"a", "b"}) MyClass aBLocal;
      @TestAccumulationPredicate("a && b") MyClass aAndBLocal;
      @TestAccumulationPredicate("a || b && c") MyClass bAndCOrALocal;
      @TestAccumulationPredicate("a || (b && c)") MyClass bAndCOrAParensLocal;
      @TestAccumulationPredicate("a && b || c") MyClass aAndBOrCLocal;
      @TestAccumulationPredicate("(a && b) || c") MyClass aAndBOrCParensLocal;
      @TestAccumulationPredicate("(a || b) && c") MyClass aOrBAndCLocal;
      @TestAccumulationPredicate("a && (b || c)") MyClass bOrCAndALocal;
      @TestAccumulationPredicate("b && c") MyClass bAndCLocal;
      @TestAccumulationPredicate("(b && c)") MyClass bAndCParensLocal;

      cmALocal = cmA;
      cmALocal = cmpA;
      // :: error: assignment.type.incompatible
      cmALocal = aOrB;
      cmALocal = aB;
      // The next line would not fail if predicate subtyping was decided by implication.
      // :: error: assignment.type.incompatible
      cmALocal = aAndB;
      // :: error: assignment.type.incompatible
      cmALocal = bAndCOrA;
      // :: error: assignment.type.incompatible
      cmALocal = bAndCOrAParens;
      // :: error: assignment.type.incompatible
      cmALocal = aAndBOrC;
      // :: error: assignment.type.incompatible
      cmALocal = aAndBOrCParens;
      // :: error: assignment.type.incompatible
      cmALocal = aOrBAndC;
      // The next line would not fail if predicate subtyping was decided by implication.
      // :: error: assignment.type.incompatible
      cmALocal = bOrCAndA;
      // :: error: assignment.type.incompatible
      cmALocal = bAndC;
      // :: error: assignment.type.incompatible
      cmALocal = bAndCParens;

      cmpALocal = cmA;
      cmpALocal = cmpA;
      // :: error: assignment.type.incompatible
      cmpALocal = aOrB;
      cmpALocal = aB;
      // The next line would not fail if predicate subtyping was decided by implication.
      // :: error: assignment.type.incompatible
      cmpALocal = aAndB;
      // :: error: assignment.type.incompatible
      cmpALocal = bAndCOrA;
      // :: error: assignment.type.incompatible
      cmpALocal = bAndCOrAParens;
      // :: error: assignment.type.incompatible
      cmpALocal = aAndBOrC;
      // :: error: assignment.type.incompatible
      cmpALocal = aAndBOrCParens;
      // :: error: assignment.type.incompatible
      cmpALocal = aOrBAndC;
      // The next line would not fail if predicate subtyping was decided by implication.
      // :: error: assignment.type.incompatible
      cmpALocal = bOrCAndA;
      // :: error: assignment.type.incompatible
      cmpALocal = bAndC;
      // :: error: assignment.type.incompatible
      cmpALocal = bAndCParens;

      aOrBLocal = cmA;
      // The next line would not fail if predicate subtyping was decided by implication.
      // :: error: assignment.type.incompatible
      aOrBLocal = cmpA;
      aOrBLocal = aOrB;
      aOrBLocal = aB;
      // The next line would not fail if predicate subtyping was decided by implication.
      // :: error: assignment.type.incompatible
      aOrBLocal = aAndB;
      // The next line would not fail if predicate subtyping was decided by implication.
      // :: error: assignment.type.incompatible
      aOrBLocal = bAndCOrA;
      // The next line would not fail if predicate subtyping was decided by implication.
      // :: error: assignment.type.incompatible
      aOrBLocal = bAndCOrAParens;
      // :: error: assignment.type.incompatible
      aOrBLocal = aAndBOrC;
      // :: error: assignment.type.incompatible
      aOrBLocal = aAndBOrCParens;
      // The next line would not fail if predicate subtyping was decided by implication.
      // :: error: assignment.type.incompatible
      aOrBLocal = aOrBAndC;
      // The next line would not fail if predicate subtyping was decided by implication.
      // :: error: assignment.type.incompatible
      aOrBLocal = bOrCAndA;
      // The next line would not fail if predicate subtyping was decided by implication.
      // :: error: assignment.type.incompatible
      aOrBLocal = bAndC;
      // The next line would not fail if predicate subtyping was decided by implication.
      // :: error: assignment.type.incompatible
      aOrBLocal = bAndCParens;

      // :: error: (assignment.type.incompatible)
      aBLocal = cmA;
      // :: error: (assignment.type.incompatible)
      aBLocal = cmpA;
      // :: error: (assignment.type.incompatible)
      aBLocal = aOrB;
      aBLocal = aB;
      aBLocal = aAndB;
      // :: error: (assignment.type.incompatible)
      aBLocal = bAndCOrA;
      // :: error: (assignment.type.incompatible)
      aBLocal = bAndCOrAParens;
      // :: error: (assignment.type.incompatible)
      aBLocal = aAndBOrC;
      // :: error: (assignment.type.incompatible)
      aBLocal = aAndBOrCParens;
      // :: error: (assignment.type.incompatible)
      aBLocal = aOrBAndC;
      // :: error: (assignment.type.incompatible)
      aBLocal = bOrCAndA;
      // :: error: (assignment.type.incompatible)
      aBLocal = bAndC;
      // :: error: (assignment.type.incompatible)
      aBLocal = bAndCParens;

      // :: error: (assignment.type.incompatible)
      aAndBLocal = cmA;
      // :: error: (assignment.type.incompatible)
      aAndBLocal = cmpA;
      // :: error: (assignment.type.incompatible)
      aAndBLocal = aOrB;
      aAndBLocal = aB;
      aAndBLocal = aAndB;
      // :: error: (assignment.type.incompatible)
      aAndBLocal = bAndCOrA;
      // :: error: (assignment.type.incompatible)
      aAndBLocal = bAndCOrAParens;
      // :: error: (assignment.type.incompatible)
      aAndBLocal = aAndBOrC;
      // :: error: (assignment.type.incompatible)
      aAndBLocal = aAndBOrCParens;
      // :: error: (assignment.type.incompatible)
      aAndBLocal = aOrBAndC;
      // :: error: (assignment.type.incompatible)
      aAndBLocal = bOrCAndA;
      // :: error: (assignment.type.incompatible)
      aAndBLocal = bAndC;
      // :: error: (assignment.type.incompatible)
      aAndBLocal = bAndCParens;

      bAndCOrALocal = cmA;
      // The next line would not fail if predicate subtyping was decided by implication.
      // :: error: assignment.type.incompatible
      bAndCOrALocal = cmpA;
      // :: error: (assignment.type.incompatible)
      bAndCOrALocal = aOrB;
      bAndCOrALocal = aB;
      // The next line would not fail if predicate subtyping was decided by implication.
      // :: error: assignment.type.incompatible
      bAndCOrALocal = aAndB;
      bAndCOrALocal = bAndCOrA;
      // The next line would not fail if predicate subtyping was decided by implication.
      // :: error: assignment.type.incompatible
      bAndCOrALocal = bAndCOrAParens;
      // :: error: (assignment.type.incompatible)
      bAndCOrALocal = aAndBOrC;
      // :: error: (assignment.type.incompatible)
      bAndCOrALocal = aAndBOrCParens;
      // The next line would not fail if predicate subtyping was decided by implication.
      // :: error: assignment.type.incompatible
      bAndCOrALocal = aOrBAndC;
      // The next line would not fail if predicate subtyping was decided by implication.
      // :: error: assignment.type.incompatible
      bAndCOrALocal = bOrCAndA;
      // The next line would not fail if predicate subtyping was decided by implication.
      // :: error: assignment.type.incompatible
      bAndCOrALocal = bAndC;
      // The next line would not fail if predicate subtyping was decided by implication.
      // :: error: assignment.type.incompatible
      bAndCOrALocal = bAndCParens;

      bAndCOrAParensLocal = cmA;
      // The next line would not fail if predicate subtyping was decided by implication.
      // :: error: assignment.type.incompatible
      bAndCOrAParensLocal = cmpA;
      // :: error: (assignment.type.incompatible)
      bAndCOrAParensLocal = aOrB;
      bAndCOrAParensLocal = aB;
      // The next line would not fail if predicate subtyping was decided by implication.
      // :: error: assignment.type.incompatible
      bAndCOrAParensLocal = aAndB;
      // The next line would not fail if predicate subtyping was decided by implication.
      // :: error: assignment.type.incompatible
      bAndCOrAParensLocal = bAndCOrA;
      bAndCOrAParensLocal = bAndCOrAParens;
      // :: error: (assignment.type.incompatible)
      bAndCOrAParensLocal = aAndBOrC;
      // :: error: (assignment.type.incompatible)
      bAndCOrAParensLocal = aAndBOrCParens;
      // The next line would not fail if predicate subtyping was decided by implication.
      // :: error: assignment.type.incompatible
      bAndCOrAParensLocal = aOrBAndC;
      // The next line would not fail if predicate subtyping was decided by implication.
      // :: error: assignment.type.incompatible
      bAndCOrAParensLocal = bOrCAndA;
      // The next line would not fail if predicate subtyping was decided by implication.
      // :: error: assignment.type.incompatible
      bAndCOrAParensLocal = bAndC;
      // The next line would not fail if predicate subtyping was decided by implication.
      // :: error: assignment.type.incompatible
      bAndCOrAParensLocal = bAndCParens;

      // :: error: (assignment.type.incompatible)
      aAndBOrCLocal = cmA;
      // :: error: (assignment.type.incompatible)
      aAndBOrCLocal = cmpA;
      // :: error: (assignment.type.incompatible)
      aAndBOrCLocal = aOrB;
      aAndBOrCLocal = aB;
      // The next line would not fail if predicate subtyping was decided by implication.
      // :: error: assignment.type.incompatible
      aAndBOrCLocal = aAndB;
      // :: error: (assignment.type.incompatible)
      aAndBOrCLocal = bAndCOrA;
      // :: error: (assignment.type.incompatible)
      aAndBOrCLocal = bAndCOrAParens;
      aAndBOrCLocal = aAndBOrC;
      // The next line would not fail if predicate subtyping was decided by implication.
      // :: error: assignment.type.incompatible
      aAndBOrCLocal = aAndBOrCParens;
      // The next line would not fail if predicate subtyping was decided by implication.
      // :: error: assignment.type.incompatible
      aAndBOrCLocal = aOrBAndC;
      // The next line would not fail if predicate subtyping was decided by implication.
      // :: error: assignment.type.incompatible
      aAndBOrCLocal = bOrCAndA;
      // The next line would not fail if predicate subtyping was decided by implication.
      // :: error: assignment.type.incompatible
      aAndBOrCLocal = bAndC;
      // The next line would not fail if predicate subtyping was decided by implication.
      // :: error: assignment.type.incompatible
      aAndBOrCLocal = bAndCParens;

      // :: error: (assignment.type.incompatible)
      aAndBOrCParensLocal = cmA;
      // :: error: (assignment.type.incompatible)
      aAndBOrCParensLocal = cmpA;
      // :: error: (assignment.type.incompatible)
      aAndBOrCParensLocal = aOrB;
      aAndBOrCParensLocal = aB;
      // The next line would not fail if predicate subtyping was decided by implication.
      // :: error: assignment.type.incompatible
      aAndBOrCParensLocal = aAndB;
      // :: error: (assignment.type.incompatible)
      aAndBOrCParensLocal = bAndCOrA;
      // :: error: (assignment.type.incompatible)
      aAndBOrCParensLocal = bAndCOrAParens;
      // The next line would not fail if predicate subtyping was decided by implication.
      // :: error: assignment.type.incompatible
      aAndBOrCParensLocal = aAndBOrC;
      aAndBOrCParensLocal = aAndBOrCParens;
      // The next line would not fail if predicate subtyping was decided by implication.
      // :: error: assignment.type.incompatible
      aAndBOrCParensLocal = aOrBAndC;
      // The next line would not fail if predicate subtyping was decided by implication.
      // :: error: assignment.type.incompatible
      aAndBOrCParensLocal = bOrCAndA;
      // The next line would not fail if predicate subtyping was decided by implication.
      // :: error: assignment.type.incompatible
      aAndBOrCParensLocal = bAndC;
      // The next line would not fail if predicate subtyping was decided by implication.
      // :: error: assignment.type.incompatible
      aAndBOrCParensLocal = bAndCParens;

      // :: error: (assignment.type.incompatible)
      aOrBAndCLocal = cmA;
      // :: error: (assignment.type.incompatible)
      aOrBAndCLocal = cmpA;
      // :: error: (assignment.type.incompatible)
      aOrBAndCLocal = aOrB;
      // :: error: (assignment.type.incompatible)
      aOrBAndCLocal = aB;
      // :: error: (assignment.type.incompatible)
      aOrBAndCLocal = aAndB;
      // :: error: (assignment.type.incompatible)
      aOrBAndCLocal = bAndCOrA;
      // :: error: (assignment.type.incompatible)
      aOrBAndCLocal = bAndCOrAParens;
      // :: error: (assignment.type.incompatible)
      aOrBAndCLocal = aAndBOrC;
      // :: error: (assignment.type.incompatible)
      aOrBAndCLocal = aAndBOrCParens;
      aOrBAndCLocal = aOrBAndC;
      // :: error: (assignment.type.incompatible)
      aOrBAndCLocal = bOrCAndA;
      // The next line would not fail if predicate subtyping was decided by implication.
      // :: error: assignment.type.incompatible
      aOrBAndCLocal = bAndC;
      // The next line would not fail if predicate subtyping was decided by implication.
      // :: error: assignment.type.incompatible
      aOrBAndCLocal = bAndCParens;

      // :: error: (assignment.type.incompatible)
      bOrCAndALocal = cmA;
      // :: error: (assignment.type.incompatible)
      bOrCAndALocal = cmpA;
      // :: error: (assignment.type.incompatible)
      bOrCAndALocal = aOrB;
      bOrCAndALocal = aB;
      // The next line would not fail if predicate subtyping was decided by implication.
      // :: error: assignment.type.incompatible
      bOrCAndALocal = aAndB;
      // :: error: (assignment.type.incompatible)
      bOrCAndALocal = bAndCOrA;
      // :: error: (assignment.type.incompatible)
      bOrCAndALocal = bAndCOrAParens;
      // :: error: (assignment.type.incompatible)
      bOrCAndALocal = aAndBOrC;
      // :: error: (assignment.type.incompatible)
      bOrCAndALocal = aAndBOrCParens;
      // :: error: (assignment.type.incompatible)
      bOrCAndALocal = aOrBAndC;
      bOrCAndALocal = bOrCAndA;
      // :: error: (assignment.type.incompatible)
      bOrCAndALocal = bAndC;
      // :: error: (assignment.type.incompatible)
      bOrCAndALocal = bAndCParens;

      // :: error: (assignment.type.incompatible)
      bAndCLocal = cmA;
      // :: error: (assignment.type.incompatible)
      bAndCLocal = cmpA;
      // :: error: (assignment.type.incompatible)
      bAndCLocal = aOrB;
      // :: error: (assignment.type.incompatible)
      bAndCLocal = aB;
      // :: error: (assignment.type.incompatible)
      bAndCLocal = aAndB;
      // :: error: (assignment.type.incompatible)
      bAndCLocal = bAndCOrA;
      // :: error: (assignment.type.incompatible)
      bAndCLocal = bAndCOrAParens;
      // :: error: (assignment.type.incompatible)
      bAndCLocal = aAndBOrC;
      // :: error: (assignment.type.incompatible)
      bAndCLocal = aAndBOrCParens;
      // :: error: (assignment.type.incompatible)
      bAndCLocal = aOrBAndC;
      // :: error: (assignment.type.incompatible)
      bAndCLocal = bOrCAndA;
      bAndCLocal = bAndC;
      // The next line would not fail if predicate subtyping was decided by implication.
      // :: error: assignment.type.incompatible
      bAndCLocal = bAndCParens;

      // :: error: (assignment.type.incompatible)
      bAndCParensLocal = cmA;
      // :: error: (assignment.type.incompatible)
      bAndCParensLocal = cmpA;
      // :: error: (assignment.type.incompatible)
      bAndCParensLocal = aOrB;
      // :: error: (assignment.type.incompatible)
      bAndCParensLocal = aB;
      // :: error: (assignment.type.incompatible)
      bAndCParensLocal = aAndB;
      // :: error: (assignment.type.incompatible)
      bAndCParensLocal = bAndCOrA;
      // :: error: (assignment.type.incompatible)
      bAndCParensLocal = bAndCOrAParens;
      // :: error: (assignment.type.incompatible)
      bAndCParensLocal = aAndBOrC;
      // :: error: (assignment.type.incompatible)
      bAndCParensLocal = aAndBOrCParens;
      // :: error: (assignment.type.incompatible)
      bAndCParensLocal = aOrBAndC;
      // :: error: (assignment.type.incompatible)
      bAndCParensLocal = bOrCAndA;
      // The next line would not fail if predicate subtyping was decided by implication.
      // :: error: assignment.type.incompatible
      bAndCParensLocal = bAndC;
      bAndCParensLocal = bAndCParens;
    }
  }
}
