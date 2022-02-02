import org.checkerframework.checker.lock.qual.GuardSatisfied;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.checkerframework.checker.lock.qual.GuardedByUnknown;
import org.checkerframework.checker.lock.qual.MayReleaseLocks;

public class GuardSatisfiedTest {
  void testGuardSatisfiedIndexMatching(
      @GuardSatisfied GuardSatisfiedTest this,
      @GuardSatisfied(1) Object o,
      @GuardSatisfied(2) Object p,
      @GuardSatisfied Object q) {
    methodToCall1(o, o);
    methodToCall1(p, p);
    // :: error: (guardsatisfied.parameters.must.match)
    methodToCall1(o, p);
    // :: error: (guardsatisfied.parameters.must.match)
    methodToCall1(p, o);
  }

  // Test defaulting of parameters - they must default to @GuardedBy({}), not @GuardSatisfied
  void testDefaulting(Object mustDefaultToGuardedByNothing, @GuardSatisfied Object p) {
    // Must assign in this direction to test the defaulting because assigning a RHS of
    // @GuardedBy({}) to a LHS @GuardSatisfied is legal.
    // :: error: (assignment)
    mustDefaultToGuardedByNothing = p;
    @GuardedBy({}) Object q = mustDefaultToGuardedByNothing;
  }

  void testMethodCall(
      @GuardSatisfied GuardSatisfiedTest this,
      @GuardedBy("lock1") Object o,
      @GuardedBy("lock2") Object p,
      @GuardSatisfied Object q) {
    // Test matching parameters

    // :: error: (lock.not.held)
    methodToCall1(o, o);
    // :: error: (lock.not.held) :: error: (guardsatisfied.parameters.must.match)
    methodToCall1(o, p);
    // :: error: (lock.not.held)
    methodToCall1(p, p);
    synchronized (lock2) {
      // :: error: (lock.not.held)
      methodToCall1(o, o);
      // :: error: (guardsatisfied.parameters.must.match) :: error: (lock.not.held)
      methodToCall1(o, p);
      methodToCall1(p, p);
      synchronized (lock1) {
        methodToCall1(o, o);
        // :: error: (guardsatisfied.parameters.must.match)
        methodToCall1(o, p);
        methodToCall1(p, p);
      }
    }

    // Test a return type matching a parameter.

    // :: error: (lock.not.held)
    o = methodToCall2(o);
    // :: error: (lock.not.held) :: error: (assignment)
    p = methodToCall2(o);
    // :: error: (lock.not.held)
    methodToCall2(o);
    // :: error: (lock.not.held)
    methodToCall2(p);
    synchronized (lock2) {
      // :: error: (lock.not.held)
      o = methodToCall2(o);
      // :: error: (lock.not.held) :: error: (assignment)
      p = methodToCall2(o);
      // :: error: (lock.not.held)
      methodToCall2(o);
      methodToCall2(p);
    }
    synchronized (lock1) {
      o = methodToCall2(o);
      // :: error: (assignment)
      p = methodToCall2(o);
      methodToCall2(o);
      // :: error: (lock.not.held)
      methodToCall2(p);
    }

    // Test the receiver type matching a parameter

    // Two @GS parameters with no index are incomparable (as is the case for 'this' and 'q').
    // :: error: (guardsatisfied.parameters.must.match)
    methodToCall3(q);

    // :: error: (guardsatisfied.parameters.must.match) :: error: (lock.not.held)
    methodToCall3(p);
    synchronized (lock1) {
      // Two @GS parameters with no index are incomparable (as is the case for 'this' and 'q').
      // :: error: (guardsatisfied.parameters.must.match)
      methodToCall3(q);
      // :: error: (guardsatisfied.parameters.must.match) :: error: (lock.not.held)
      methodToCall3(p);
      synchronized (lock2) {
        // Two @GS parameters with no index are incomparable (as is the case for 'this' and 'q').
        // :: error: (guardsatisfied.parameters.must.match)
        methodToCall3(q);
        // :: error: (guardsatisfied.parameters.must.match)
        methodToCall3(p);
      }
    }

    // Test the return type matching the receiver type

    methodToCall4();
  }

  // Test the return type NOT matching the receiver type
  void testMethodCall(@GuardedBy("lock1") GuardSatisfiedTest this) {
    @GuardedBy("lock2") Object g;
    // :: error: (lock.not.held)
    methodToCall4();
    // TODO: lock.not.held is getting swallowed below
    //  error (assignment) error (lock.not.held)
    // g = methodToCall4();

    // Separate the above test case into two for now
    // :: error: (lock.not.held)
    methodToCall4();

    // The following error is due to the fact that you cannot access "this.lock1" without first
    // having acquired "lock1".  The right fix in a user scenario would be to not guard "this"
    // with "this.lock1". The current object could instead be guarded by "<self>" or by some
    // other lock expression that is not one of its fields. We are keeping this test case here
    // to make sure this scenario issues a warning.
    // :: error: (lock.not.held)
    synchronized (lock1) {
      // :: error: (assignment)
      g = methodToCall4();
    }
  }

  // :: error: (guardsatisfied.return.must.have.index)
  @GuardSatisfied Object testReturnTypesMustMatchAndMustHaveAnIndex1(@GuardSatisfied Object o) {
    // If the two @GuardSatisfied had an index, this error would not be issued:
    // :: error: (guardsatisfied.assignment.disallowed)
    return o;
  }

  @GuardSatisfied(1) Object testReturnTypesMustMatchAndMustHaveAnIndex2(@GuardSatisfied(1) Object o) {
    return o;
  }

  @GuardSatisfied(0) Object testReturnTypesMustMatchAndMustHaveAnIndex3(@GuardSatisfied(0) Object o) {
    return o;
  }

  // @GuardSatisfied is equivalent to @GuardSatisfied(-1).
  // :: error: (guardsatisfied.return.must.have.index)
  @GuardSatisfied Object testReturnTypesMustMatchAndMustHaveAnIndex4(@GuardSatisfied(-1) Object o) {
    // If the two @GuardSatisfied had an index, this error would not be issued:
    // :: error: (guardsatisfied.assignment.disallowed)
    return o;
  }

  @GuardSatisfied(1) Object testReturnTypesMustMatchAndMustHaveAnIndex5(@GuardSatisfied(2) Object o) {
    // :: error: (return)
    return o;
  }

  // :: error: (guardsatisfied.return.must.have.index)
  @GuardSatisfied Object testReturnTypesMustMatchAndMustHaveAnIndex6(@GuardSatisfied(2) Object o) {
    // :: error: (return)
    return o;
  }

  void testParamsMustMatch(@GuardSatisfied(1) Object o, @GuardSatisfied(2) Object p) {
    // :: error: (assignment)
    o = p;
  }

  void methodToCall1(
      @GuardSatisfied GuardSatisfiedTest this,
      @GuardSatisfied(1) Object o,
      @GuardSatisfied(1) Object p) {}

  @GuardSatisfied(1) Object methodToCall2(@GuardSatisfied GuardSatisfiedTest this, @GuardSatisfied(1) Object o) {
    return o;
  }

  void methodToCall3(@GuardSatisfied(1) GuardSatisfiedTest this, @GuardSatisfied(1) Object o) {}

  @GuardSatisfied(1) Object methodToCall4(@GuardSatisfied(1) GuardSatisfiedTest this) {
    return this;
  }

  final Object lock1 = new Object();
  final Object lock2 = new Object();

  // This method exists to prevent flow-sensitive refinement.
  @GuardedBy({"lock1", "lock2"}) Object guardedByLock1Lock2() {
    return new Object();
  }

  void testAssignment(@GuardSatisfied Object o) {
    @GuardedBy({"lock1", "lock2"}) Object p = guardedByLock1Lock2();
    // :: error: (lock.not.held)
    o = p;
    synchronized (lock1) {
      // :: error: (lock.not.held)
      o = p;
      synchronized (lock2) {
        o = p;
      }
    }
  }

  // Test disallowed @GuardSatisfied locations.
  // Whenever a disallowed location can be located within a method return type, receiver or
  // parameter, test it there, because it's important to check that those are not mistakenly
  // allowed, since annotations on method return types, receivers and parameters are allowed.  By
  // definition, fields and non-parameter local variables cannot be in one of these locations on a
  // method declaration, but other locations can be.

  // :: error: (guardsatisfied.location.disallowed)
  @GuardSatisfied Object field;
  // :: error: (guardsatisfied.location.disallowed)
  void testGuardSatisfiedOnArrayElementAndLocalVariable(@GuardSatisfied Object[] array) {
    // :: error: (guardsatisfied.location.disallowed)
    @GuardSatisfied Object local;
  }

  // :: error: (guardsatisfied.location.disallowed)
  <T extends @GuardSatisfied Object> T testGuardSatisfiedOnBound(T t) {
    return t;
  }

  class MyParameterizedClass1<T extends @GuardedByUnknown Object> {
    void testGuardSatisfiedOnReceiverOfParameterizedClass(
        @GuardSatisfied MyParameterizedClass1<T> this) {}

    void testGuardSatisfiedOnArrayOfParameterizedType(
        MyParameterizedClass1<T> @GuardSatisfied [] array) {}

    void testGuardSatisfiedOnArrayComponentOfParameterizedType(
        // :: error: (guardsatisfied.location.disallowed)
        @GuardSatisfied MyParameterizedClass1<T>[] array) {}
  }

  void testGuardSatisfiedOnWildCardExtendsBound(
      // :: error: (guardsatisfied.location.disallowed)
      MyParameterizedClass1<? extends @GuardSatisfied Object> l) {}

  void testGuardSatisfiedOnWildCardSuperBound(
      // :: error: (guardsatisfied.location.disallowed)
      MyParameterizedClass1<? super @GuardSatisfied String> l) {}

  @GuardSatisfied(1) Object testGuardSatisfiedOnParameters(
      @GuardSatisfied GuardSatisfiedTest this,
      Object @GuardSatisfied [] array,
      @GuardSatisfied Object param,
      @GuardSatisfied(1) Object param2) {
    return param2;
  }

  void testGuardSatisfiedOnArray1(Object @GuardSatisfied [][][] array) {}
  // :: error: (guardsatisfied.location.disallowed)
  void testGuardSatisfiedOnArray2(@GuardSatisfied Object[][][] array) {}
  // :: error: (guardsatisfied.location.disallowed)
  void testGuardSatisfiedOnArray3(Object[] @GuardSatisfied [][] array) {}
  // :: error: (guardsatisfied.location.disallowed)
  void testGuardSatisfiedOnArray4(Object[][] @GuardSatisfied [] array) {}
}

class Foo {
  @MayReleaseLocks
  void m1() {}

  @MayReleaseLocks
  // :: error: (guardsatisfied.with.mayreleaselocks)
  void m2(@GuardSatisfied Foo f) {
    // :: error: (method.invocation)
    f.m1();
  }

  @MayReleaseLocks
  void m2_2(Foo f) {
    f.m1();
  }

  void m3(@GuardSatisfied Foo f) {
    // :: error: (method.guarantee.violated) :: error: (method.invocation)
    f.m1();
  }

  @MayReleaseLocks
  void m4(Foo f) {
    f.m1();
  }

  @MayReleaseLocks
  void m5(Foo f) {
    m3(f);
  }
}
