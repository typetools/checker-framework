import org.checkerframework.framework.qual.DefaultQualifier;
import org.checkerframework.framework.qual.TypeUseLocation;
import org.checkerframework.framework.testchecker.h1h2checker.quals.*;

// Test defaulting behavior, e.g. that local variables, casts, and instanceof
// propagate the type of the respective sub-expression and that upper bounds
// are separately annotated.
public class Defaulting {

  @DefaultQualifier(
      value = H1S1.class,
      locations = {TypeUseLocation.LOCAL_VARIABLE})
  class TestLocal {
    void m(@H1S1 Object p1, @H1S2 Object p2) {
      Object l1 = p1;
      // :: error: (assignment)
      Object l2 = p2;
    }
  }

  @DefaultQualifier(
      value = H1Top.class,
      locations = {TypeUseLocation.LOCAL_VARIABLE})
  @DefaultQualifier(
      value = H1S1.class,
      locations = {TypeUseLocation.UPPER_BOUND})
  @DefaultQualifier(
      value = H1S2.class,
      locations = {TypeUseLocation.OTHERWISE})
  // Type of x is <@H1S2 X extends @H1S1 Object>, these annotations are siblings
  // and should not be in the same bound
  // :: warning: (inconsistent.constructor.type) :: error: (bound) :: error: (super.invocation)
  class TestUpperBound<X extends Object> {
    void m(X p) {
      @H1S1 Object l1 = p;
      // :: error: (assignment)
      @H1S2 Object l2 = p;
      Object l3 = p;
    }
  }

  @DefaultQualifier(
      value = H1Top.class,
      locations = {TypeUseLocation.LOCAL_VARIABLE})
  @DefaultQualifier(
      value = H1S1.class,
      locations = {TypeUseLocation.PARAMETER})
  @DefaultQualifier(
      value = H1S2.class,
      locations = {TypeUseLocation.OTHERWISE})
  // :: warning: (inconsistent.constructor.type) :: error: (super.invocation)
  class TestParameter {
    void m(Object p) {
      @H1S1 Object l1 = p;
      // :: error: (assignment)
      @H1S2 Object l2 = p;
      Object l3 = p;
    }

    void call() {
      // :: warning: (cast.unsafe.constructor.invocation)
      m(new @H1S1 Object());
      // :: error: (argument) :: warning: (cast.unsafe.constructor.invocation)
      m(new @H1S2 Object());
      // :: error: (argument)
      m(new Object());
    }
  }

  @DefaultQualifier(
      value = H1Top.class,
      locations = {TypeUseLocation.LOCAL_VARIABLE})
  @DefaultQualifier(
      value = H1S1.class,
      locations = {TypeUseLocation.PARAMETER})
  @DefaultQualifier(
      value = H1S2.class,
      locations = {TypeUseLocation.OTHERWISE})
  class TestConstructorParameter {

    // :: warning: (inconsistent.constructor.type) :: error: (super.invocation)
    TestConstructorParameter(Object p) {
      @H1S1 Object l1 = p;
      // :: error: (assignment)
      @H1S2 Object l2 = p;
      Object l3 = p;
    }

    void call() {
      // :: warning: (cast.unsafe.constructor.invocation)
      new TestConstructorParameter(new @H1S1 Object());
      // :: error: (argument) :: warning: (cast.unsafe.constructor.invocation)
      new TestConstructorParameter(new @H1S2 Object());
      // :: error: (argument)
      new TestConstructorParameter(new Object());
    }
  }

  @DefaultQualifier(
      value = H1Top.class,
      locations = {TypeUseLocation.LOCAL_VARIABLE})
  @DefaultQualifier(
      value = H1S1.class,
      locations = {TypeUseLocation.RETURN})
  @DefaultQualifier(
      value = H1S2.class,
      locations = {TypeUseLocation.OTHERWISE})
  // :: warning: (inconsistent.constructor.type) :: error: (super.invocation)
  class TestReturns {
    Object res() {
      // :: warning: (cast.unsafe.constructor.invocation)
      return new @H1S1 Object();
    }

    void m() {
      @H1S1 Object l1 = res();
      // :: error: (assignment)
      @H1S2 Object l2 = res();
      Object l3 = res();
    }

    Object res2() {
      // :: error: (return) :: warning: (cast.unsafe.constructor.invocation)
      return new @H1S2 Object();
    }

    Object res3() {
      // :: error: (return)
      return new Object();
    }
  }

  @DefaultQualifier(
      value = H1Top.class,
      locations = {TypeUseLocation.LOCAL_VARIABLE})
  @DefaultQualifier(
      value = H1S1.class,
      locations = {TypeUseLocation.RECEIVER})
  public class ReceiverDefaulting {
    public ReceiverDefaulting() {}

    public void m() {}
  }

  @DefaultQualifier(
      value = H1Top.class,
      locations = {TypeUseLocation.LOCAL_VARIABLE})
  class TestReceiver {

    void call() {
      // :: warning: (cast.unsafe.constructor.invocation)
      @H1S1 ReceiverDefaulting r2 = new @H1S1 ReceiverDefaulting();
      // :: warning: (cast.unsafe.constructor.invocation)
      @H1S2 ReceiverDefaulting r3 = new @H1S2 ReceiverDefaulting();
      ReceiverDefaulting r = new ReceiverDefaulting();

      r2.m();
      // :: error: (method.invocation)
      r3.m();
      // :: error: (method.invocation)
      r.m();
    }
  }
}
