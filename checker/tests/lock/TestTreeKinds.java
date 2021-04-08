import java.util.Random;
import java.util.concurrent.locks.ReentrantLock;
import org.checkerframework.checker.lock.qual.*;
import org.checkerframework.dataflow.qual.SideEffectFree;

public class TestTreeKinds {
  class MyClass {
    Object field = new Object();

    @LockingFree
    Object method(@GuardSatisfied MyClass this) {
      return new Object();
    }

    void method2(@GuardSatisfied MyClass this) {}
  }

  @GuardedBy("lock") MyClass m;

  {
    // In constructor/initializer, it's OK not to hold the lock on 'this', but other locks must
    // be respected.
    // :: error: (lock.not.held)
    m.field = new Object();
  }

  final ReentrantLock lock = new ReentrantLock();
  final ReentrantLock lock2 = new ReentrantLock();

  @GuardedBy("lock") MyClass foo = new MyClass();

  MyClass unguardedFoo = new MyClass();

  @EnsuresLockHeld("lock")
  void lockTheLock() {
    lock.lock();
  }

  @EnsuresLockHeld("lock2")
  void lockTheLock2() {
    lock2.lock();
  }

  @EnsuresLockHeldIf(expression = "lock", result = true)
  boolean tryToLockTheLock() {
    return lock.tryLock();
  }

  // This @MayReleaseLocks annotation causes dataflow analysis to assume 'lock' is released after
  // unlockTheLock() is called.
  @MayReleaseLocks
  void unlockTheLock() {}

  @SideEffectFree
  void sideEffectFreeMethod() {}

  @LockingFree
  void lockingFreeMethod() {}

  @MayReleaseLocks
  void nonSideEffectFreeMethod() {}

  @Holding("lock")
  void requiresLockHeldMethod() {}

  MyClass fooArray @GuardedBy("lock") [] = new MyClass[3];

  @GuardedBy("lock") MyClass fooArray2[] = new MyClass[3];

  @GuardedBy("lock") MyClass fooArray3[][] = new MyClass[3][3];

  MyClass fooArray4 @GuardedBy("lock") [][] = new MyClass[3][3];

  MyClass fooArray5[] @GuardedBy("lock") [] = new MyClass[3][3];

  class myClass {
    int i = 0;
  }

  @GuardedBy("lock") myClass myClassInstance = new myClass();

  @GuardedBy("lock") Exception exception = new Exception();

  class MyParametrizedType<T> {
    T foo;
    int l;
  }

  @GuardedBy("lock") MyParametrizedType<MyClass> myParametrizedType = new MyParametrizedType<>();

  MyClass getFooWithWrongReturnType() {
    // :: error: (return.type.incompatible)
    return foo; // return of guarded object
  }

  @GuardedBy("lock") MyClass getFoo() {
    return foo;
  }

  MyClass @GuardedBy("lock") [] getFooArray() {
    return fooArray;
  }

  @GuardedBy("lock") MyClass[] getFooArray2() {
    return fooArray2;
  }

  @GuardedBy("lock") MyClass[][] getFooArray3() {
    return fooArray3;
  }

  MyClass @GuardedBy("lock") [][] getFooArray4() {
    return fooArray4;
  }

  MyClass[] @GuardedBy("lock") [] getFooArray5() {
    return fooArray5;
  }

  enum myEnumType {
    ABC,
    DEF
  }

  @GuardedBy("lock") myEnumType myEnum;

  void testEnumType() {
    // TODO: assignment.type.incompatible is technically correct, but we could
    // make it friendlier for the user if constant enum values on the RHS
    // automatically cast to the @GuardedBy annotation of the LHS.
    // :: error: (assignment.type.incompatible)
    myEnum = myEnumType.ABC;
  }

  final Object intrinsicLock = new Object();

  void testThreadHoldsLock(@GuardedBy("intrinsicLock") MyClass m) {
    if (Thread.holdsLock(intrinsicLock)) {
      m.field.toString();
    } else {
      // :: error: (lock.not.held)
      m.field.toString();
    }
  }

  void testTreeTypes() {
    int i, l;

    MyClass o = new MyClass();
    MyClass f = new MyClass();

    // The following test cases were inspired from annotator.find.ASTPathCriterion.isSatisfiedBy
    // in the Annotation File Utilities

    // TODO: File a bug for the dataflow issue mentioned in the line below.
    // TODO: uncomment: Hits a bug in dataflow:
    // do {
    //     break;
    // } while (foo.field != null); // access to guarded object in condition of do/while loop
    // :: error: (lock.not.held)
    for (foo = new MyClass(); foo.field != null; foo = new MyClass()) {
      break;
    } // access to guarded object in condition of for loop
    // assignment to guarded object (OK) --- foo is still refined to @GuardedBy("lock") after
    // this point, though.
    foo = new MyClass();
    // A simple method call to a guarded object is not considered a dereference (only field
    // accesses are considered dereferences).
    unguardedFoo.method2();
    // Same as above, but the guard must be satisfied if the receiver is @GuardSatisfied.
    // :: error: (lock.not.held)
    foo.method2();
    // attempt to use guarded object in a switch statement
    // :: error: (lock.not.held)
    switch (foo.field.hashCode()) {
    }
    // attempt to use guarded object inside a try with resources
    // try(foo = new MyClass()) { foo.field.toString(); }

    // Retrieving an element from a guarded array is a dereference
    // :: error: (lock.not.held)
    MyClass m = fooArray[0];

    // method call on dereference of unguarded element of *guarded* array
    // :: error: (lock.not.held)
    fooArray[0].field.toString();
    // :: error: (lock.not.held)
    l = fooArray.length; // dereference of guarded array itself

    // method call on dereference of guarded array element
    // :: error: (lock.not.held)
    fooArray2[0].field.toString();
    // method call on dereference of unguarded array - TODO: currently preconditions are not
    // retrieved correctly from array types. This is not unique to the Lock Checker.
    // fooArray2.field.toString();

    // method call on dereference of guarded array element of multidimensional array
    // :: error: (lock.not.held)
    fooArray3[0][0].field.toString();
    // method call on dereference of unguarded single-dimensional array element of unguarded
    // multidimensional array - TODO: currently preconditions are not retrieved correctly from
    // array types. This is not unique to the Lock Checker.
    // fooArray3[0].field.toString();
    // method call on dereference of unguarded multidimensional array - TODO: currently
    // preconditions are not retrieved correctly from array types. This is not unique to the
    // Lock Checker.
    // fooArray3.field.toString();

    // method call on dereference of unguarded array element of *guarded* multidimensional array
    // :: error: (lock.not.held)
    fooArray4[0][0].field.toString();
    // dereference of unguarded single-dimensional array element of *guarded* multidimensional
    // array
    // :: error: (lock.not.held)
    l = fooArray4[0].length;
    // dereference of guarded multidimensional array
    // :: error: (lock.not.held)
    l = fooArray4.length;

    // method call on dereference of unguarded array element of *guarded subarray* of
    // multidimensional array
    // :: error: (lock.not.held)
    fooArray5[0][0].field.toString();
    // dereference of guarded single-dimensional array element of multidimensional array
    // :: error: (lock.not.held)
    l = fooArray5[0].length;
    // dereference of unguarded multidimensional array
    l = fooArray5.length;

    // :: error: (lock.not.held)
    l = getFooArray().length; // dereference of guarded array returned by a method

    // method call on dereference of guarded array element returned by a method
    // :: error: (lock.not.held)
    getFooArray2()[0].field.toString();
    // dereference of unguarded array returned by a method
    l = getFooArray2().length;

    // method call on dereference of guarded array element of multidimensional array returned by
    // a method
    // :: error: (lock.not.held)
    getFooArray3()[0][0].field.toString();
    // dereference of unguarded single-dimensional array element of multidimensional array
    // returned by a method
    l = getFooArray3()[0].length;
    // dereference of unguarded multidimensional array returned by a method
    l = getFooArray3().length;

    // method call on dereference of unguarded array element of *guarded* multidimensional array
    // returned by a method
    // :: error: (lock.not.held)
    getFooArray4()[0][0].field.toString();
    // dereference of unguarded single-dimensional array element of *guarded* multidimensional
    // array returned by a method
    // :: error: (lock.not.held)
    l = getFooArray4()[0].length;
    // dereference of guarded multidimensional array returned by a method
    // :: error: (lock.not.held)
    l = getFooArray4().length;

    // method call on dereference of unguarded array element of *guarded subarray* of
    // multidimensional array returned by a method
    // :: error: (lock.not.held)
    getFooArray5()[0][0].field.toString();
    // dereference of guarded single-dimensional array element of multidimensional array
    // returned by a method
    // :: error: (lock.not.held)
    l = getFooArray5()[0].length;
    // dereference of unguarded multidimensional array returned by a method
    l = getFooArray5().length;

    // Test different @GuardedBy(...) present on the element and array locations.
    @GuardedBy("lock") MyClass @GuardedBy("lock2") [] array = new MyClass[3];
    // :: error: (lock.not.held)
    array[0].field = new Object();
    if (lock.isHeldByCurrentThread()) {
      // :: error: (lock.not.held)
      array[0].field = new Object();
      if (lock2.isHeldByCurrentThread()) {
        array[0].field = new Object();
      }
    }

    // method call on guarded object within parenthesized expression
    // :: error: (lock.not.held)
    String s = (foo.field.toString());
    // :: error: (lock.not.held)
    foo.field.toString(); // method call on guarded object
    // :: error: (lock.not.held)
    getFoo().field.toString(); // method call on guarded object returned by a method
    // :: error: (lock.not.held)
    this.foo.field.toString(); // method call on guarded object using 'this' literal
    // dereference of guarded object in labeled statement
    label:
    // :: error: (lock.not.held)
    foo.field.toString();
    // access to guarded object in instanceof expression (OK)
    if (foo instanceof MyClass) {}
    // access to guarded object in while condition of while loop (OK)
    while (foo != null) {
      break;
    }
    // binary operator on guarded object in else if condition (OK)
    if (false) {
    } else if (foo == o) {
    }
    // access to guarded object in a lambda expression
    Runnable rn =
        () -> {
          // :: error: (lock.not.held)
          foo.field.toString();
        };
    // :: error: (lock.not.held)
    i = myClassInstance.i; // access to member field of guarded object
    // MemberReferenceTrees? how do they work
    fooArray = new MyClass[3]; // second allocation of guarded array (OK)
    // dereference of guarded object in conditional expression tree
    // :: error: (lock.not.held)
    s = i == 5 ? foo.field.toString() : f.field.toString();
    // dereference of guarded object in conditional expression tree
    // :: error: (lock.not.held)
    s = i == 5 ? f.field.toString() : foo.field.toString();
    // Testing of 'return' is done in getFooWithWrongReturnType()
    // throwing a guarded object - when throwing an exception, it must be @GuardedBy({}). Even
    // @GuardedByUnknown is not allowed.
    try {
      // :: error: (throw.type.invalid)
      throw exception;
    } catch (Exception e) {
    }
    // casting of a guarded object to an unguarded object
    // :: error: (assignment.type.incompatible)
    @GuardedBy({}) Object e1 = (Object) exception;
    // OK, since the local variable's type gets refined to @GuardedBy("lock")
    Object e2 = (Object) exception;
    // :: error: (lock.not.held)
    l = myParametrizedType.l; // dereference of guarded object having a parameterized type

    // We need to support locking on local variables and protecting local variables because
    // these locals may contain references to fields. Somehow we need to pass along the
    // information of which field it was.

    if (foo == o) { // binary operator on guarded object (OK)
      o.field.toString();
    }

    if (foo == null) {
      // With -AconcurrentSemantics turned off, a cannot.dereference error would be expected,
      // since there is an attempt to dereference an expression whose type has been refined to
      // @GuardedByBottom (due to the comparison to null). However, with -AconcurrentSemantics
      // turned on, foo may no longer be null by now, the refinement to @GuardedByBottom is
      // lost and the refined type of foo is now the declared type ( @GuardedBy("lock") ),
      // resulting in the lock.not.held error.
      // :: error: (lock.not.held)
      foo.field.toString();
    }

    // TODO: Reenable:
    // @PolyGuardedBy should not be written here, but it is not explicitly forbidden by the
    // framework.
    // @PolyGuardedBy MyClass m2 = new MyClass();
    // (cannot.dereference)
    // m2.field.toString();
  }

  @MayReleaseLocks
  public void testLocals() {
    final ReentrantLock localLock = new ReentrantLock();

    @GuardedBy("localLock") MyClass guardedByLocalLock = new MyClass();

    // :: error: (lock.not.held)
    guardedByLocalLock.field.toString();

    @GuardedBy("lock") MyClass local = new MyClass();

    // :: error: (lock.not.held)
    local.field.toString();

    lockTheLock();

    local.field.toString(); // No warning output

    unlockTheLock();
  }

  @MayReleaseLocks
  public void testMethodAnnotations() {
    Random r = new Random();

    if (r.nextBoolean()) {
      lockTheLock();
      requiresLockHeldMethod();
    } else {
      // :: error: (contracts.precondition.not.satisfied)
      requiresLockHeldMethod();
    }

    if (r.nextBoolean()) {
      lockTheLock();
      foo.field.toString();

      unlockTheLock();

      // :: error: (lock.not.held)
      foo.field.toString();
    } else {
      // :: error: (lock.not.held)
      foo.field.toString();
    }

    if (tryToLockTheLock()) {
      foo.field.toString();
    } else {
      // :: error: (lock.not.held)
      foo.field.toString();
    }

    if (r.nextBoolean()) {
      lockTheLock();
      sideEffectFreeMethod();
      foo.field.toString();
    } else {
      lockTheLock();
      nonSideEffectFreeMethod();
      // :: error: (lock.not.held)
      foo.field.toString();
    }

    if (r.nextBoolean()) {
      lockTheLock();
      lockingFreeMethod();
      foo.field.toString();
    } else {
      lockTheLock();
      nonSideEffectFreeMethod();
      // :: error: (lock.not.held)
      foo.field.toString();
    }
  }

  void methodThatTakesAnInteger(Integer i) {}

  void testBoxedPrimitiveType() {
    Integer i = null;
    if (i == null) {}

    methodThatTakesAnInteger(i);
  }

  void testReceiverGuardedByItself(@GuardedBy("<self>") TestTreeKinds this) {
    // :: error: (lock.not.held)
    method();
    synchronized (this) {
      method();
    }
  }

  void method(@GuardSatisfied TestTreeKinds this) {}

  void testOtherClassReceiverGuardedByItself(final @GuardedBy("<self>") OtherClass o) {
    // :: error: (lock.not.held)
    o.foo();
    synchronized (o) {
      o.foo();
    }
  }

  class OtherClass {
    void foo(@GuardSatisfied OtherClass this) {}
  }

  void testExplicitLockSynchronized() {
    final ReentrantLock lock = new ReentrantLock();
    // :: error: (explicit.lock.synchronized)
    synchronized (lock) {
    }
  }

  void testPrimitiveTypeGuardedby() {
    // :: error: (immutable.type.guardedby)
    @GuardedBy("lock") int a = 0;
    // :: error: (immutable.type.guardedby)
    @GuardedBy int b = 0;
    // :: error: (immutable.type.guardedby) :: error: (guardsatisfied.location.disallowed)
    @GuardSatisfied int c = 0;
    // :: error: (immutable.type.guardedby) :: error: (guardsatisfied.location.disallowed)
    @GuardSatisfied(1) int d = 0;
    int e = 0;
    // :: error: (immutable.type.guardedby)
    @GuardedByUnknown int f = 0;
    // :: error: (immutable.type.guardedby) :: error: (assignment.type.incompatible)
    @GuardedByBottom int g = 0;
  }

  void testBinaryOperatorBooleanResultIsAlwaysGuardedByNothing() {
    @GuardedBy("lock") Object o1 = new Object();
    Object o2 = new Object();
    // boolean variables are implicitly @GuardedBy({}).
    boolean b1 = o1 == o2;
    boolean b2 = o2 == o1;
    boolean b3 = o1 != o2;
    boolean b4 = o2 != o1;
    boolean b5 = o1 instanceof Object;
    boolean b6 = o2 instanceof Object;
    boolean b7 = o1 instanceof @GuardedBy("lock") Object;
    boolean b8 = o2 instanceof @GuardedBy("lock") Object;
  }
}
