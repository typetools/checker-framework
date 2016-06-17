import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.locks.ReentrantLock;
import org.checkerframework.checker.lock.qual.*;
import org.checkerframework.dataflow.qual.SideEffectFree;

public class TestTreeKinds {
  class MyClass {
     Object field = new Object();
     @LockingFree
     Object method(@GuardSatisfied MyClass this) {return new Object();}
     void method2(@GuardSatisfied MyClass this) {}
  }

  @GuardedBy("lock") MyClass m;
  {
    //:: error: (contracts.precondition.not.satisfied.field)
    m.field = new Object(); // In constructor/initializer, it's OK not to hold the lock on 'this', but other locks must be respected.
  }

  final ReentrantLock lock = new ReentrantLock();
  final ReentrantLock lock2 = new ReentrantLock();

  @GuardedBy("lock")
  MyClass foo = new MyClass();

  MyClass unguardedFoo = new MyClass();

  @EnsuresLockHeld("lock")
  void lockTheLock() {
    lock.lock();
  }

  @EnsuresLockHeld("lock2")
  void lockTheLock2() {
    lock2.lock();
  }

  @EnsuresLockHeldIf(expression="lock", result=true)
  boolean tryToLockTheLock() {
    return lock.tryLock();
  }

  @MayReleaseLocks // This @MayReleaseLocks annotation causes dataflow analysis to assume 'lock' is released after unlockTheLock() is called.
  void unlockTheLock() {
  }

  @SideEffectFree
  void sideEffectFreeMethod() {
  }

  @LockingFree
  void lockingFreeMethod() {
  }

  @MayReleaseLocks
  void nonSideEffectFreeMethod() {
  }

  @Holding("lock")
  void requiresLockHeldMethod() {
  }

  MyClass fooArray @GuardedBy("lock") [] = new MyClass[3];

  @GuardedBy("lock")
  MyClass fooArray2[] = new MyClass[3];

  @GuardedBy("lock")
  MyClass fooArray3[][] = new MyClass[3][3];

  MyClass fooArray4 @GuardedBy("lock") [][] = new MyClass[3][3];

  MyClass fooArray5[] @GuardedBy("lock") [] = new MyClass[3][3];

  class myClass {
    int i = 0;
  }

  @GuardedBy("lock")
  myClass myClassInstance = new myClass();

  @GuardedBy("lock")
  Exception exception = new Exception();

  class MyParametrizedType<T> {
    T foo;
    int l;
  }

  @GuardedBy("lock")
  MyParametrizedType<MyClass> myParametrizedType = new MyParametrizedType<MyClass>();

  MyClass getFooWithWrongReturnType() {
    //:: error: (return.type.incompatible)
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
    ABC, DEF
  }

  @GuardedBy("lock")
  myEnumType myEnum;

void testEnumType() {
    //:: error: (assignment.type.incompatible)
    myEnum = myEnumType.ABC; // TODO: assignment.type.incompatible is technically correct, but we could make it friendlier for the user if constant enum values on the RHS
                             // automatically cast to the @GuardedBy annotation of the LHS
}

final Object intrinsicLock = new Object();

void testThreadHoldsLock(@GuardedBy("intrinsicLock") MyClass m) {
    if (Thread.holdsLock(intrinsicLock)) {
        m.field.toString();
    } else {
        //:: error: (contracts.precondition.not.satisfied.field)
        m.field.toString();
    }
}

void testTreeTypes() {
    int i, l;

    MyClass o = new MyClass();
    MyClass f = new MyClass();

    // The following test cases were inspired from annotator.find.ASTPathCriterion.isSatisfiedBy in the Annotation File Utilities

    // TODO: File a bug for the dataflow issue mentioned in the line below.
    // TODO: uncomment: Hits a bug in dataflow:    do { break; } while (foo.field != null); // access to guarded object in while condition of do/while loop
    //:: error: (contracts.precondition.not.satisfied.field)
    for (foo = new MyClass(); foo.field != null; foo = new MyClass()) { break; } // access to guarded object in condition of for loop
    foo = new MyClass(); // assignment to guarded object (OK) --- foo is still refined to @GuardedBy("lock") after this point, though.
    unguardedFoo.method2(); // A simple method call to a guarded object is not considered a dereference (only field accesses are considered dereferences).
    //:: error: (contracts.precondition.not.satisfied)
    foo.method2(); // Same as above, but the guard must be satisfied if the receiver is @GuardSatisfied.
    //:: error: (contracts.precondition.not.satisfied.field)
    switch(foo.field.hashCode()) { // attempt to use guarded object in a switch statement
    }
    // try(foo = new MyClass()) { foo.field.toString(); } // attempt to use guarded object inside a try with resources

    // Retrieving an element from a guarded array is a dereference
    //:: error: (contracts.precondition.not.satisfied.field)
    MyClass m = fooArray[0];

    //:: error: (contracts.precondition.not.satisfied.field)
    fooArray[0].field.toString(); // method call on dereference of unguarded element of *guarded* array
    //:: error: (contracts.precondition.not.satisfied.field)
    l = fooArray.length; // dereference of guarded array itself

    //:: error: (contracts.precondition.not.satisfied.field)
    fooArray2[0].field.toString(); // method call on dereference of guarded array element
    // fooArray2.field.toString(); // method call on dereference of unguarded array - TODO: currently preconditions are not retrieved correctly from array types. This is not unique to the Lock Checker.

    //:: error: (contracts.precondition.not.satisfied.field)
    fooArray3[0][0].field.toString(); // method call on dereference of guarded array element of multidimensional array
    // fooArray3[0].field.toString(); // method call on dereference of unguarded single-dimensional array element of unguarded multidimensional array - TODO: currently preconditions are not retrieved correctly from array types. This is not unique to the Lock Checker.
    // fooArray3.field.toString(); // method call on dereference of unguarded multidimensional array - TODO: currently preconditions are not retrieved correctly from array types. This is not unique to the Lock Checker.

    //:: error: (contracts.precondition.not.satisfied.field)
    fooArray4[0][0].field.toString(); // method call on dereference of unguarded array element of *guarded* multidimensional array
    //:: error: (contracts.precondition.not.satisfied.field)
    l = fooArray4[0].length; // dereference of unguarded single-dimensional array element of *guarded* multidimensional array
    //:: error: (contracts.precondition.not.satisfied.field)
    l = fooArray4.length; // dereference of guarded multidimensional array

    //:: error: (contracts.precondition.not.satisfied.field)
    fooArray5[0][0].field.toString(); // method call on dereference of unguarded array element of *guarded subarray* of multidimensional array
    //:: error: (contracts.precondition.not.satisfied.field)
    l = fooArray5[0].length; // dereference of guarded single-dimensional array element of multidimensional array
    l = fooArray5.length; // dereference of unguarded multidimensional array

    //:: error: (contracts.precondition.not.satisfied)
    l = getFooArray().length; // dereference of guarded array returned by a method

    //:: error: (contracts.precondition.not.satisfied.field)
    getFooArray2()[0].field.toString(); // method call on dereference of guarded array element returned by a method
    l = getFooArray2().length; // dereference of unguarded array returned by a method

    //:: error: (contracts.precondition.not.satisfied.field)
    getFooArray3()[0][0].field.toString(); // method call on dereference of guarded array element of multidimensional array returned by a method
    l = getFooArray3()[0].length; // dereference of unguarded single-dimensional array element of multidimensional array returned by a method
    l = getFooArray3().length; // dereference of unguarded multidimensional array returned by a method

    //:: error: (contracts.precondition.not.satisfied)
    getFooArray4()[0][0].field.toString(); // method call on dereference of unguarded array element of *guarded* multidimensional array returned by a method
    //:: error: (contracts.precondition.not.satisfied)
    l = getFooArray4()[0].length; // dereference of unguarded single-dimensional array element of *guarded* multidimensional array returned by a method
    //:: error: (contracts.precondition.not.satisfied)
    l = getFooArray4().length; // dereference of guarded multidimensional array returned by a method

    //:: error: (contracts.precondition.not.satisfied.field)
    getFooArray5()[0][0].field.toString(); // method call on dereference of unguarded array element of *guarded subarray* of multidimensional array returned by a method
    //:: error: (contracts.precondition.not.satisfied.field)
    l = getFooArray5()[0].length; // dereference of guarded single-dimensional array element of multidimensional array returned by a method
    l = getFooArray5().length; // dereference of unguarded multidimensional array returned by a method

    // Test different @GuardedBy(...) present on the element and array locations.
    @GuardedBy("lock") MyClass @GuardedBy("lock2") [] array = new MyClass[3];
    //:: error: (contracts.precondition.not.satisfied.field)
    array[0].field = new Object();
    if (lock.isHeldByCurrentThread()) {
      //:: error: (contracts.precondition.not.satisfied.field)
      array[0].field = new Object();
      if (lock2.isHeldByCurrentThread()) {
        array[0].field = new Object();
      }
    }

    //:: error: (contracts.precondition.not.satisfied.field)
    String s = (foo.field.toString()); // method call on guarded object within parenthesized expression
    //:: error: (contracts.precondition.not.satisfied.field)
    foo.field.toString(); // method call on guarded object
    //:: error: (contracts.precondition.not.satisfied)
    getFoo().field.toString(); // method call on guarded object returned by a method
    //:: error: (contracts.precondition.not.satisfied.field)
    this.foo.field.toString(); // method call on guarded object using 'this' literal
    //:: error: (contracts.precondition.not.satisfied.field)
    label: foo.field.toString(); // dereference of guarded object in labeled statement
    if (foo instanceof MyClass) {} // access to guarded object in instanceof expression (OK)
    while (foo != null) { break; } // access to guarded object in while condition of while loop (OK)
    if (false) {} else if (foo == o) {} // binary operator on guarded object in else if condition (OK)
    //:: error: (contracts.precondition.not.satisfied.field)
    Runnable rn = () -> { foo.field.toString(); }; // access to guarded object in a lambda expression
    //:: error: (contracts.precondition.not.satisfied.field)
    i = myClassInstance.i; // access to member field of guarded object
    // MemberReferenceTrees? how do they work
    fooArray = new MyClass[3]; // second allocation of guarded array (OK)
    //:: error: (contracts.precondition.not.satisfied.field)
    s = i == 5 ? foo.field.toString() : f.field.toString(); // dereference of guarded object in conditional expression tree
    //:: error: (contracts.precondition.not.satisfied.field)
    s = i == 5 ? f.field.toString() : foo.field.toString(); // dereference of guarded object in conditional expression tree
    // Testing of 'return' is done in getFooWithWrongReturnType()
    //:: error: (throw.type.invalid)
    try { throw exception; } catch (Exception e) {} // throwing a guarded object - when throwing an exception, it must be @GuardedBy({}). Even @GuardedByUnknown is not allowed.
    //:: error: (assignment.type.incompatible)
    @GuardedBy({}) Object e1 = (Object) exception; // casting of a guarded object to an unguarded object
    Object e2 = (Object) exception; // OK, since the local variable's type gets refined to @GuardedBy("lock")
    //:: error: (contracts.precondition.not.satisfied.field)
    l = myParametrizedType.l; // dereference of guarded object having a parameterized type

    // We need to support locking on local variables and protecting local variables because these locals may contain references to fields. Somehow we need to pass along the information of which field it was.

    if (foo == o) { // binary operator on guarded object (OK)
      o.field.toString();
    }

    if (foo == null) {
      // With -AconcurrentSemantics turned off, a cannot.dereference error would be expected, since
      // there is an attempt to dereference an expression whose type has been refined to @GuardedByBottom
      // (due to the comparison to null). However, with -AconcurrentSemantics turned on, foo may no longer
      // be null by now, the refinement to @GuardedByBottom is lost and the refined type of foo is now the
      // declared type ( @GuardedBy("lock") ), resulting in the contracts.precondition.not.satisfied.field error.
      //:: error: (contracts.precondition.not.satisfied.field)
      foo.field.toString();
    }

    // TODO: Reenable:
    // @PolyGuardedBy should not be written here, but it is not explicitly forbidden by the framework.
    // @PolyGuardedBy MyClass m2 = new MyClass();
    // (cannot.dereference)
    // m2.field.toString();
}

@MayReleaseLocks
public void testLocals() {
  final ReentrantLock localLock = new ReentrantLock();

  @GuardedBy("localLock")
  MyClass guardedByLocalLock = new MyClass();

  //:: error: (contracts.precondition.not.satisfied.field)
  guardedByLocalLock.field.toString();

  @GuardedBy("lock")
  MyClass local = new MyClass();

  //:: error: (contracts.precondition.not.satisfied.field)
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
    //:: error: (contracts.precondition.not.satisfied)
    requiresLockHeldMethod();
  }

  if (r.nextBoolean()) {
    lockTheLock();
    foo.field.toString();

    unlockTheLock();

    //:: error: (contracts.precondition.not.satisfied.field)
    foo.field.toString();
  } else {
    //:: error: (contracts.precondition.not.satisfied.field)
    foo.field.toString();
  }

  if (tryToLockTheLock()) {
    foo.field.toString();
  } else {
    //:: error: (contracts.precondition.not.satisfied.field)
    foo.field.toString();
  }

  // An error is raised here because non-side-effect-free method unlockTheLock() is called above,
  // and is due to this line in LockStore.updateForMethodCall:
  // localVariableValues.clear();
  // which clears the value of 'r' in the store to its CLIMB-to-top default of @GuardedByUnknown.
  // TODO: Fix LockStore.updateForMethodCall so it is less conservative and remove
  // the expected error.
  //:: error: (method.invocation.invalid)
  if (r.nextBoolean()) {
    lockTheLock();
    sideEffectFreeMethod();
    foo.field.toString();
  } else {
    lockTheLock();
    nonSideEffectFreeMethod();
    //:: error: (contracts.precondition.not.satisfied.field)
    foo.field.toString();
  }

  // An error is raised here because the non-side-effect-free methods unlockTheLock() and nonSideEffectFreeMethod() are called above
  // (for the method.invocation.invalid error not to be issued, neither of those two methods can be called above),
  // and is due to this line in LockStore.updateForMethodCall:
  // localVariableValues.clear();
  // which clears the value of 'r' in the store to its CLIMB-to-top default of @GuardedByUnknown.
  // TODO: Fix LockStore.updateForMethodCall so it is less conservative and remove
  // the expected error.
  //:: error: (method.invocation.invalid)
  if (r.nextBoolean()) {
    lockTheLock();
    lockingFreeMethod();
    foo.field.toString();
  } else {
    lockTheLock();
    nonSideEffectFreeMethod();
    //:: error: (contracts.precondition.not.satisfied.field)
    foo.field.toString();
  }
}

  void methodThatTakesAnInteger(Integer i) { }

  void testBoxedPrimitiveType() {
    Integer i = null;
    if (i == null) {
    }

    methodThatTakesAnInteger(i);
  }

  void testReceiverGuardedByItself(@GuardedBy("<self>") TestTreeKinds this) {
      //:: error: (contracts.precondition.not.satisfied)
      method();
      synchronized(this) {
          method();
      }
  }
  void method(@GuardSatisfied TestTreeKinds this) {
  }

  void testOtherClassReceiverGuardedByItself(final @GuardedBy("<self>") OtherClass o) {
     //:: error: (contracts.precondition.not.satisfied)
     o.foo();
     synchronized(o) {
         o.foo();
     }
  }

  class OtherClass {
      void foo(@GuardSatisfied OtherClass this) {}
   }

  void testExplicitLockSynchronized() {
     final ReentrantLock lock = new ReentrantLock();
     //:: error: (explicit.lock.synchronized)
     synchronized(lock) {
     }
  }

  void testPrimitiveTypeGuardedby() {
      //:: error: (primitive.type.guardedby)
      @GuardedBy("lock") int a = 0;
      //:: error: (primitive.type.guardedby)
      @GuardedBy int b = 0;
      //:: error: (primitive.type.guardedby) :: error: (guardsatisfied.location.disallowed)
      @GuardSatisfied int c = 0;
      //:: error: (primitive.type.guardedby) :: error: (guardsatisfied.location.disallowed)
      @GuardSatisfied(1) int d = 0;
      int e = 0;
      //:: error: (primitive.type.guardedby)
      @GuardedByUnknown int f = 0;
      //:: error: (primitive.type.guardedby) :: error: (assignment.type.incompatible)
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
