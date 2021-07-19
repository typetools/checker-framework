import org.checkerframework.framework.testchecker.testaccumulation.qual.*;

public class Predicates {

  void testOr1() {
    MyClass m1 = new MyClass();

    // :: error: method.invocation
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

    // :: error: method.invocation
    m1.d();
  }

  void testAnd2() {
    MyClass m1 = new MyClass();

    m1.a();
    // :: error: method.invocation
    m1.d();
  }

  void testAnd3() {
    MyClass m1 = new MyClass();

    m1.b();
    // :: error: method.invocation
    m1.d();
  }

  void testAnd4() {
    MyClass m1 = new MyClass();

    m1.a();
    m1.c();
    // :: error: method.invocation
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

    // :: error: method.invocation
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
    // :: error: method.invocation
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

    // :: error: method.invocation
    m1.f();
  }

  void testPrecedence2() {
    MyClass m1 = new MyClass();

    m1.a();
    // :: error: method.invocation
    m1.f();
  }

  void testPrecedence3() {
    MyClass m1 = new MyClass();

    m1.b();
    // :: error: method.invocation
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
      // :: error: method.invocation
      cAble.d();
      // :: error: method.invocation
      cAble.e();
      // :: error: method.invocation
      cAble.f();
    }

    static void testAssignability2(@TestAccumulationPredicate("a && b") MyClass dAble) {
      // These calls would work if subtyping between predicates was by implication. They issue
      // errors, because it is not.
      // :: error: method.invocation
      dAble.c();
      dAble.d();
      // :: error: method.invocation
      dAble.e();
      // :: error: method.invocation
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
      // :: error: assignment
      cmALocal = aOrB;
      cmALocal = aB;
      // The next line would not fail if predicate subtyping was decided by implication.
      // :: error: assignment
      cmALocal = aAndB;
      // :: error: assignment
      cmALocal = bAndCOrA;
      // :: error: assignment
      cmALocal = bAndCOrAParens;
      // :: error: assignment
      cmALocal = aAndBOrC;
      // :: error: assignment
      cmALocal = aAndBOrCParens;
      // :: error: assignment
      cmALocal = aOrBAndC;
      // The next line would not fail if predicate subtyping was decided by implication.
      // :: error: assignment
      cmALocal = bOrCAndA;
      // :: error: assignment
      cmALocal = bAndC;
      // :: error: assignment
      cmALocal = bAndCParens;

      cmpALocal = cmA;
      cmpALocal = cmpA;
      // :: error: assignment
      cmpALocal = aOrB;
      cmpALocal = aB;
      // The next line would not fail if predicate subtyping was decided by implication.
      // :: error: assignment
      cmpALocal = aAndB;
      // :: error: assignment
      cmpALocal = bAndCOrA;
      // :: error: assignment
      cmpALocal = bAndCOrAParens;
      // :: error: assignment
      cmpALocal = aAndBOrC;
      // :: error: assignment
      cmpALocal = aAndBOrCParens;
      // :: error: assignment
      cmpALocal = aOrBAndC;
      // The next line would not fail if predicate subtyping was decided by implication.
      // :: error: assignment
      cmpALocal = bOrCAndA;
      // :: error: assignment
      cmpALocal = bAndC;
      // :: error: assignment
      cmpALocal = bAndCParens;

      aOrBLocal = cmA;
      // The next line would not fail if predicate subtyping was decided by implication.
      // :: error: assignment
      aOrBLocal = cmpA;
      aOrBLocal = aOrB;
      aOrBLocal = aB;
      // The next line would not fail if predicate subtyping was decided by implication.
      // :: error: assignment
      aOrBLocal = aAndB;
      // The next line would not fail if predicate subtyping was decided by implication.
      // :: error: assignment
      aOrBLocal = bAndCOrA;
      // The next line would not fail if predicate subtyping was decided by implication.
      // :: error: assignment
      aOrBLocal = bAndCOrAParens;
      // :: error: assignment
      aOrBLocal = aAndBOrC;
      // :: error: assignment
      aOrBLocal = aAndBOrCParens;
      // The next line would not fail if predicate subtyping was decided by implication.
      // :: error: assignment
      aOrBLocal = aOrBAndC;
      // The next line would not fail if predicate subtyping was decided by implication.
      // :: error: assignment
      aOrBLocal = bOrCAndA;
      // The next line would not fail if predicate subtyping was decided by implication.
      // :: error: assignment
      aOrBLocal = bAndC;
      // The next line would not fail if predicate subtyping was decided by implication.
      // :: error: assignment
      aOrBLocal = bAndCParens;

      // :: error: (assignment)
      aBLocal = cmA;
      // :: error: (assignment)
      aBLocal = cmpA;
      // :: error: (assignment)
      aBLocal = aOrB;
      aBLocal = aB;
      aBLocal = aAndB;
      // :: error: (assignment)
      aBLocal = bAndCOrA;
      // :: error: (assignment)
      aBLocal = bAndCOrAParens;
      // :: error: (assignment)
      aBLocal = aAndBOrC;
      // :: error: (assignment)
      aBLocal = aAndBOrCParens;
      // :: error: (assignment)
      aBLocal = aOrBAndC;
      // :: error: (assignment)
      aBLocal = bOrCAndA;
      // :: error: (assignment)
      aBLocal = bAndC;
      // :: error: (assignment)
      aBLocal = bAndCParens;

      // :: error: (assignment)
      aAndBLocal = cmA;
      // :: error: (assignment)
      aAndBLocal = cmpA;
      // :: error: (assignment)
      aAndBLocal = aOrB;
      aAndBLocal = aB;
      aAndBLocal = aAndB;
      // :: error: (assignment)
      aAndBLocal = bAndCOrA;
      // :: error: (assignment)
      aAndBLocal = bAndCOrAParens;
      // :: error: (assignment)
      aAndBLocal = aAndBOrC;
      // :: error: (assignment)
      aAndBLocal = aAndBOrCParens;
      // :: error: (assignment)
      aAndBLocal = aOrBAndC;
      // :: error: (assignment)
      aAndBLocal = bOrCAndA;
      // :: error: (assignment)
      aAndBLocal = bAndC;
      // :: error: (assignment)
      aAndBLocal = bAndCParens;

      bAndCOrALocal = cmA;
      // The next line would not fail if predicate subtyping was decided by implication.
      // :: error: assignment
      bAndCOrALocal = cmpA;
      // :: error: (assignment)
      bAndCOrALocal = aOrB;
      bAndCOrALocal = aB;
      // The next line would not fail if predicate subtyping was decided by implication.
      // :: error: assignment
      bAndCOrALocal = aAndB;
      bAndCOrALocal = bAndCOrA;
      // The next line would not fail if predicate subtyping was decided by implication.
      // :: error: assignment
      bAndCOrALocal = bAndCOrAParens;
      // :: error: (assignment)
      bAndCOrALocal = aAndBOrC;
      // :: error: (assignment)
      bAndCOrALocal = aAndBOrCParens;
      // The next line would not fail if predicate subtyping was decided by implication.
      // :: error: assignment
      bAndCOrALocal = aOrBAndC;
      // The next line would not fail if predicate subtyping was decided by implication.
      // :: error: assignment
      bAndCOrALocal = bOrCAndA;
      // The next line would not fail if predicate subtyping was decided by implication.
      // :: error: assignment
      bAndCOrALocal = bAndC;
      // The next line would not fail if predicate subtyping was decided by implication.
      // :: error: assignment
      bAndCOrALocal = bAndCParens;

      bAndCOrAParensLocal = cmA;
      // The next line would not fail if predicate subtyping was decided by implication.
      // :: error: assignment
      bAndCOrAParensLocal = cmpA;
      // :: error: (assignment)
      bAndCOrAParensLocal = aOrB;
      bAndCOrAParensLocal = aB;
      // The next line would not fail if predicate subtyping was decided by implication.
      // :: error: assignment
      bAndCOrAParensLocal = aAndB;
      // The next line would not fail if predicate subtyping was decided by implication.
      // :: error: assignment
      bAndCOrAParensLocal = bAndCOrA;
      bAndCOrAParensLocal = bAndCOrAParens;
      // :: error: (assignment)
      bAndCOrAParensLocal = aAndBOrC;
      // :: error: (assignment)
      bAndCOrAParensLocal = aAndBOrCParens;
      // The next line would not fail if predicate subtyping was decided by implication.
      // :: error: assignment
      bAndCOrAParensLocal = aOrBAndC;
      // The next line would not fail if predicate subtyping was decided by implication.
      // :: error: assignment
      bAndCOrAParensLocal = bOrCAndA;
      // The next line would not fail if predicate subtyping was decided by implication.
      // :: error: assignment
      bAndCOrAParensLocal = bAndC;
      // The next line would not fail if predicate subtyping was decided by implication.
      // :: error: assignment
      bAndCOrAParensLocal = bAndCParens;

      // :: error: (assignment)
      aAndBOrCLocal = cmA;
      // :: error: (assignment)
      aAndBOrCLocal = cmpA;
      // :: error: (assignment)
      aAndBOrCLocal = aOrB;
      aAndBOrCLocal = aB;
      // The next line would not fail if predicate subtyping was decided by implication.
      // :: error: assignment
      aAndBOrCLocal = aAndB;
      // :: error: (assignment)
      aAndBOrCLocal = bAndCOrA;
      // :: error: (assignment)
      aAndBOrCLocal = bAndCOrAParens;
      aAndBOrCLocal = aAndBOrC;
      // The next line would not fail if predicate subtyping was decided by implication.
      // :: error: assignment
      aAndBOrCLocal = aAndBOrCParens;
      // The next line would not fail if predicate subtyping was decided by implication.
      // :: error: assignment
      aAndBOrCLocal = aOrBAndC;
      // The next line would not fail if predicate subtyping was decided by implication.
      // :: error: assignment
      aAndBOrCLocal = bOrCAndA;
      // The next line would not fail if predicate subtyping was decided by implication.
      // :: error: assignment
      aAndBOrCLocal = bAndC;
      // The next line would not fail if predicate subtyping was decided by implication.
      // :: error: assignment
      aAndBOrCLocal = bAndCParens;

      // :: error: (assignment)
      aAndBOrCParensLocal = cmA;
      // :: error: (assignment)
      aAndBOrCParensLocal = cmpA;
      // :: error: (assignment)
      aAndBOrCParensLocal = aOrB;
      aAndBOrCParensLocal = aB;
      // The next line would not fail if predicate subtyping was decided by implication.
      // :: error: assignment
      aAndBOrCParensLocal = aAndB;
      // :: error: (assignment)
      aAndBOrCParensLocal = bAndCOrA;
      // :: error: (assignment)
      aAndBOrCParensLocal = bAndCOrAParens;
      // The next line would not fail if predicate subtyping was decided by implication.
      // :: error: assignment
      aAndBOrCParensLocal = aAndBOrC;
      aAndBOrCParensLocal = aAndBOrCParens;
      // The next line would not fail if predicate subtyping was decided by implication.
      // :: error: assignment
      aAndBOrCParensLocal = aOrBAndC;
      // The next line would not fail if predicate subtyping was decided by implication.
      // :: error: assignment
      aAndBOrCParensLocal = bOrCAndA;
      // The next line would not fail if predicate subtyping was decided by implication.
      // :: error: assignment
      aAndBOrCParensLocal = bAndC;
      // The next line would not fail if predicate subtyping was decided by implication.
      // :: error: assignment
      aAndBOrCParensLocal = bAndCParens;

      // :: error: (assignment)
      aOrBAndCLocal = cmA;
      // :: error: (assignment)
      aOrBAndCLocal = cmpA;
      // :: error: (assignment)
      aOrBAndCLocal = aOrB;
      // :: error: (assignment)
      aOrBAndCLocal = aB;
      // :: error: (assignment)
      aOrBAndCLocal = aAndB;
      // :: error: (assignment)
      aOrBAndCLocal = bAndCOrA;
      // :: error: (assignment)
      aOrBAndCLocal = bAndCOrAParens;
      // :: error: (assignment)
      aOrBAndCLocal = aAndBOrC;
      // :: error: (assignment)
      aOrBAndCLocal = aAndBOrCParens;
      aOrBAndCLocal = aOrBAndC;
      // :: error: (assignment)
      aOrBAndCLocal = bOrCAndA;
      // The next line would not fail if predicate subtyping was decided by implication.
      // :: error: assignment
      aOrBAndCLocal = bAndC;
      // The next line would not fail if predicate subtyping was decided by implication.
      // :: error: assignment
      aOrBAndCLocal = bAndCParens;

      // :: error: (assignment)
      bOrCAndALocal = cmA;
      // :: error: (assignment)
      bOrCAndALocal = cmpA;
      // :: error: (assignment)
      bOrCAndALocal = aOrB;
      bOrCAndALocal = aB;
      // The next line would not fail if predicate subtyping was decided by implication.
      // :: error: assignment
      bOrCAndALocal = aAndB;
      // :: error: (assignment)
      bOrCAndALocal = bAndCOrA;
      // :: error: (assignment)
      bOrCAndALocal = bAndCOrAParens;
      // :: error: (assignment)
      bOrCAndALocal = aAndBOrC;
      // :: error: (assignment)
      bOrCAndALocal = aAndBOrCParens;
      // :: error: (assignment)
      bOrCAndALocal = aOrBAndC;
      bOrCAndALocal = bOrCAndA;
      // :: error: (assignment)
      bOrCAndALocal = bAndC;
      // :: error: (assignment)
      bOrCAndALocal = bAndCParens;

      // :: error: (assignment)
      bAndCLocal = cmA;
      // :: error: (assignment)
      bAndCLocal = cmpA;
      // :: error: (assignment)
      bAndCLocal = aOrB;
      // :: error: (assignment)
      bAndCLocal = aB;
      // :: error: (assignment)
      bAndCLocal = aAndB;
      // :: error: (assignment)
      bAndCLocal = bAndCOrA;
      // :: error: (assignment)
      bAndCLocal = bAndCOrAParens;
      // :: error: (assignment)
      bAndCLocal = aAndBOrC;
      // :: error: (assignment)
      bAndCLocal = aAndBOrCParens;
      // :: error: (assignment)
      bAndCLocal = aOrBAndC;
      // :: error: (assignment)
      bAndCLocal = bOrCAndA;
      bAndCLocal = bAndC;
      // The next line would not fail if predicate subtyping was decided by implication.
      // :: error: assignment
      bAndCLocal = bAndCParens;

      // :: error: (assignment)
      bAndCParensLocal = cmA;
      // :: error: (assignment)
      bAndCParensLocal = cmpA;
      // :: error: (assignment)
      bAndCParensLocal = aOrB;
      // :: error: (assignment)
      bAndCParensLocal = aB;
      // :: error: (assignment)
      bAndCParensLocal = aAndB;
      // :: error: (assignment)
      bAndCParensLocal = bAndCOrA;
      // :: error: (assignment)
      bAndCParensLocal = bAndCOrAParens;
      // :: error: (assignment)
      bAndCParensLocal = aAndBOrC;
      // :: error: (assignment)
      bAndCParensLocal = aAndBOrCParens;
      // :: error: (assignment)
      bAndCParensLocal = aOrBAndC;
      // :: error: (assignment)
      bAndCParensLocal = bOrCAndA;
      // The next line would not fail if predicate subtyping was decided by implication.
      // :: error: assignment
      bAndCParensLocal = bAndC;
      bAndCParensLocal = bAndCParens;
    }
  }
}
