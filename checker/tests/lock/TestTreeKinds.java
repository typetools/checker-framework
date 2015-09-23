import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.locks.ReentrantLock;
import org.checkerframework.checker.lock.qual.*;
import org.checkerframework.dataflow.qual.SideEffectFree;

public class TestTreeKinds {

  class MyClass {
     Object field = new Object();
     @LockingFree
     Object method(@GuardSatisfied MyClass this){return new Object();}
     void method2(){}
  }

  @GuardedBy("lock") MyClass m;
  {
    //:: error: (contracts.precondition.not.satisfied.field)
    m.field = new Object(); // In constructor/initializer, it's OK not to hold the lock on 'this', but other locks must be respected.
  }

  ReentrantLock lock = new ReentrantLock();
  ReentrantLock lock2 = new ReentrantLock();

  @GuardedBy("lock")
  MyClass foo = new MyClass();

  MyClass unguardedFoo = new MyClass();

  @EnsuresLockHeld("lock")
  void lockTheLock() {
    lock.lock();
  }

  @EnsuresLockHeldIf(expression="lock",result=true)
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

  /* TODO Reenable this after the Lock Checker is changed to handle primitives.
  // Test that we can Guard primitives, not just objects
  @GuardedBy("lock")
  int primitive = 1;

  @GuardedBy("lock")
  boolean primitiveBoolean;*/

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

/* TODO Reenable this after the Lock Checker is changed to handle primitives.
public void testOperationsWithPrimitives() {
    int i = 0;
    boolean b;

    // TODO reenable this error: (contracts.precondition.not.satisfied.field)
    i = i >>> primitive;
    // TODO reenable this error: (contracts.precondition.not.satisfied.field)
    i = primitive >>> i;

    // TODO reenable this error: (contracts.precondition.not.satisfied.field)
    i >>>= primitive;
    // TODO reenable this error: (contracts.precondition.not.satisfied.field)
    primitive >>>= i;

    // TODO reenable this error: (contracts.precondition.not.satisfied.field)
    i %= primitive;
    // TODO reenable this error: (contracts.precondition.not.satisfied.field)
    i = 4 % primitive;
    // TODO reenable this error: (contracts.precondition.not.satisfied.field)
    i = primitive % 4;

    // TODO reenable this error: (contracts.precondition.not.satisfied.field)
    primitive++;
    // TODO reenable this error: (contracts.precondition.not.satisfied.field)
    primitive--;
    // TODO reenable this error: (contracts.precondition.not.satisfied.field)
    ++primitive;
    // TODO reenable this error: (contracts.precondition.not.satisfied.field)
    --primitive;

    // TODO reenable this error: (contracts.precondition.not.satisfied.field)
    if (primitive != 5){ }

    // TODO reenable this error: (contracts.precondition.not.satisfied.field)
    i = primitive >> i;
    // TODO reenable this error: (contracts.precondition.not.satisfied.field)
    i = primitive << i;
    // TODO reenable this error: (contracts.precondition.not.satisfied.field)
    i = i >> primitive;
    // TODO reenable this error: (contracts.precondition.not.satisfied.field)
    i = i << primitive;

    // TODO reenable this error: (contracts.precondition.not.satisfied.field)
    i <<= primitive;
    // TODO reenable this error: (contracts.precondition.not.satisfied.field)
    i >>= primitive;
    // TODO reenable this error: (contracts.precondition.not.satisfied.field)
    primitive <<= i;
    // TODO reenable this error: (contracts.precondition.not.satisfied.field)
    primitive >>= i;

    // TODO reenable this error: (contracts.precondition.not.satisfied.field)
    assert(primitiveBoolean);

    // TODO reenable this error: (contracts.precondition.not.satisfied.field)
    b = primitive >= i;
    // TODO reenable this error: (contracts.precondition.not.satisfied.field)
    b = primitive <= i;
    // TODO reenable this error: (contracts.precondition.not.satisfied.field)
    b = primitive > i;
    // TODO reenable this error: (contracts.precondition.not.satisfied.field)
    b = primitive < i;
    // TODO reenable this error: (contracts.precondition.not.satisfied.field)
    b = i >= primitive;
    // TODO reenable this error: (contracts.precondition.not.satisfied.field)
    b = i <= primitive;
    // TODO reenable this error: (contracts.precondition.not.satisfied.field)
    b = i > primitive;
    // TODO reenable this error: (contracts.precondition.not.satisfied.field)
    b = i < primitive;

    // TODO reenable this error: (contracts.precondition.not.satisfied.field)
    i += primitive;
    // TODO reenable this error: (contracts.precondition.not.satisfied.field)
    i -= primitive;
    // TODO reenable this error: (contracts.precondition.not.satisfied.field)
    i *= primitive;
    // TODO reenable this error: (contracts.precondition.not.satisfied.field)
    i /= primitive;

    // TODO reenable this error: (contracts.precondition.not.satisfied.field)
    i = 4 + primitive;
    // TODO reenable this error: (contracts.precondition.not.satisfied.field)
    i = 4 - primitive;
    // TODO reenable this error: (contracts.precondition.not.satisfied.field)
    i = 4 * primitive;
    // TODO reenable this error: (contracts.precondition.not.satisfied.field)
    i = 4 / primitive;

    // TODO reenable this error: (contracts.precondition.not.satisfied.field)
    i = primitive + 4;
    // TODO reenable this error: (contracts.precondition.not.satisfied.field)
    i = primitive - 4;
    // TODO reenable this error: (contracts.precondition.not.satisfied.field)
    i = primitive * 4;
    // TODO reenable this error: (contracts.precondition.not.satisfied.field)
    i = primitive / 4;




    // TODO reenable this error: (contracts.precondition.not.satisfied.field)
    if (primitiveBoolean){}

    // TODO reenable this error: (contracts.precondition.not.satisfied.field)
    i = ~primitive;

    // TODO reenable this error: (contracts.precondition.not.satisfied.field)
    b = primitiveBoolean || false;
    // TODO reenable this error: (contracts.precondition.not.satisfied.field)
    b = primitiveBoolean | false;

    // TODO reenable this error: (contracts.precondition.not.satisfied.field)
    b = primitiveBoolean ^ true;

    // TODO reenable this error: (contracts.precondition.not.satisfied.field)
    b &= primitiveBoolean;

    // TODO reenable this error: (contracts.precondition.not.satisfied.field)
    b |= primitiveBoolean;

    // TODO reenable this error: (contracts.precondition.not.satisfied.field)
    b ^= primitiveBoolean;

    // TODO reenable this error: (contracts.precondition.not.satisfied.field)
    b = !primitiveBoolean;


    // TODO reenable this error: (contracts.precondition.not.satisfied.field)
    i = primitive;


    // TODO reenable this error: (contracts.precondition.not.satisfied.field)
    b = true && primitiveBoolean;
    // TODO reenable this error: (contracts.precondition.not.satisfied.field)
    b = true & primitiveBoolean;

    // TODO reenable this error: (contracts.precondition.not.satisfied.field)
    b = false || primitiveBoolean;
    // TODO reenable this error: (contracts.precondition.not.satisfied.field)
    b = false | primitiveBoolean;

    // TODO reenable this error: (contracts.precondition.not.satisfied.field)
    b = false ^ primitiveBoolean;

    // TODO reenable this error: (contracts.precondition.not.satisfied.field)
    b = primitiveBoolean && true;
    // TODO reenable this error: (contracts.precondition.not.satisfied.field)
    b = primitiveBoolean & true;
}*/

void testEnumType() {
    //:: error: (assignment.type.incompatible)
    myEnum = myEnumType.ABC; // TODO: assignment.type.incompatible is technically correct, but we could make it friendlier for the user if constant enum values on the RHS
                             // automatically cast to the @GuardedBy annotation of the LHS
}

Object intrinsicLock;

void testThreadHoldsLock(@GuardedBy("intrinsicLock") MyClass m) {
    if (Thread.holdsLock(intrinsicLock)){
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

// Hits a bug in dataflow:    do { break; } while(foo != null); // access to guarded object in while condition of do/while loop
// Hits a bug in dataflow:    for(foo = new MyClass(); foo != null; foo = new MyClass()){ break; } // access to guarded object in condition of for loop
    foo = new MyClass(); // assignment to guarded object (OK) --- foo is still refined to @GuardedBy("lock") after this point, though.
    unguardedFoo.method2(); // A simple method call to a guarded object is not considered a dereference (only field accesses are considered dereferences).
    //:: error: (contracts.precondition.not.satisfied)
    foo.method2(); // Same as above, but the guard must be satisfied if the receiver is @GuardSatisfied, which is the default.
    // TODO: Make the synchronized expression count as a dereference even if it is not a MemberSelectTree --- :: error: (contracts.precondition.not.satisfied.field)
    synchronized(foo){ // attempt to use guarded object as a lock - this counts as a dereference of foo
    }
    //:: error: (contracts.precondition.not.satisfied.field)
    switch(foo.field.hashCode()){ // attempt to use guarded object in a switch statement
    }
    // try(foo = new MyClass()){ foo.field.toString(); } // attempt to use guarded object inside a try with resources

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
    if (foo instanceof MyClass){} // access to guarded object in instanceof expression (OK)
    while(foo != null){ break; } // access to guarded object in while condition of while loop (OK)
    if (false){}else if (foo == o){} // binary operator on guarded object in else if condition (OK)
    // Hits a bug in dataflow:    Runnable rn = () -> { foo.field.toString(); }; // access to guarded object in a lambda expression
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
    try{ throw exception; } catch(Exception e){} // throwing a guarded object - when throwing an exception, it must be @GuardedBy({}). Even @GuardedByInaccessible is not allowed.
    //:: error: (assignment.type.incompatible)
    Object e = (Object) exception; // casting of a guarded object
    //:: error: (contracts.precondition.not.satisfied.field)
    l = myParametrizedType.l; // dereference of guarded object having a parameterized type

    // We need to support locking on local variables and protecting local variables because these locals may contain references to fields. Somehow we need to pass along the information of which field it was.

    if (foo == o) { // binary operator on guarded object (OK)
      o.field.toString();
    }
    //
    if (foo == null) {
      // Attempt to dereference an expression whose type has been refined to @GuardedByBottom (due to the comparison to null)
      //:: error: (cannot.dereference)
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
  ReentrantLock localLock = new ReentrantLock();

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
  }
  else
  {
    //:: error: (contracts.precondition.not.satisfied)
    requiresLockHeldMethod();
  }

  if (r.nextBoolean()) {
    lockTheLock();
    // Yes, lock reassignments are bad, but for the purposes of this
    // experiment they are useful.
    @LockHeld ReentrantLock heldLock = lock;
    foo.field.toString();

    unlockTheLock();

    //:: error: (assignment.type.incompatible)
    @LockHeld ReentrantLock heldLock2 = lock;
    //:: error: (contracts.precondition.not.satisfied.field)
    foo.field.toString();
  }
  else {
    //:: error: (assignment.type.incompatible)
    @LockHeld ReentrantLock heldLock = lock;
    //:: error: (contracts.precondition.not.satisfied.field)
    foo.field.toString();
  }

  if (tryToLockTheLock()) {
    @LockHeld ReentrantLock heldLock = lock;
    foo.field.toString();
  }
  else {
    //:: error: (assignment.type.incompatible)
    @LockHeld ReentrantLock heldLock = lock;
    //:: error: (contracts.precondition.not.satisfied.field)
    foo.field.toString();
  }

  if (r.nextBoolean()) {
    lockTheLock();
    sideEffectFreeMethod();
    @LockHeld ReentrantLock heldLock = lock;
    foo.field.toString();
  }
  else {
    lockTheLock();
    nonSideEffectFreeMethod();
    //:: error: (assignment.type.incompatible)
    @LockHeld ReentrantLock heldLock = lock;
    //:: error: (contracts.precondition.not.satisfied.field)
    foo.field.toString();
  }

  if (r.nextBoolean()) {
    lockTheLock();
    lockingFreeMethod();
    @LockHeld ReentrantLock heldLock = lock;
    foo.field.toString();
  }
  else {
    lockTheLock();
    nonSideEffectFreeMethod();
    //:: error: (assignment.type.incompatible)
    @LockHeld ReentrantLock heldLock = lock;
    //:: error: (contracts.precondition.not.satisfied.field)
    foo.field.toString();
  }
}

}
