import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.locks.ReentrantLock;
import org.checkerframework.checker.lock.qual.*;
import org.checkerframework.dataflow.qual.SideEffectFree;

public class TestTreeKinds {
  ReentrantLock lock = new ReentrantLock();
  ReentrantLock lock2 = new ReentrantLock();

  @GuardedBy("lock")
  Object foo = new Object();

  @EnsuresLockHeld("lock")
  void lockTheLock() {
    lock.lock();
  }

  @EnsuresLockHeldIf(expression="lock",result=true)
  boolean tryToLockTheLock() {
    return lock.tryLock();
  }

  void unlockTheLock() {
  }

  @SideEffectFree
  void sideEffectFreeMethod() {
  }

  @LockingFree
  void lockingFreeMethod() {
  }

  void nonSideEffectFreeMethod() {
  }

  @Holding("lock")
  void requiresLockHeldMethod() {
  }

  Object fooArray @GuardedBy("lock") [] = new Object[3];

  @GuardedBy("lock")
  Object fooArray2[] = new Object[3];

  @GuardedBy("lock")
  Object fooArray3[][] = new Object[3][3];

  Object fooArray4 @GuardedBy("lock") [][] = new Object[3][3];

  Object fooArray5[] @GuardedBy("lock") [] = new Object[3][3];

  // Test that we can Guard primitives, not just Objects
  @GuardedBy("lock")
  int primitive = 1;

  @GuardedBy("lock")
  boolean primitiveBoolean;

  class myClass {
    int i = 0;
  }

  @GuardedBy("lock")
  myClass myClassInstance = new myClass();

  @GuardedBy("lock")
  Exception exception = new Exception();

  @GuardedBy("lock")
  ArrayList<Object> myList = new ArrayList<Object>();

  Object getFooWithWrongReturnType() {
    //:: error: (return.type.incompatible)
    return foo; // return of guarded object
  }

  @GuardedBy("lock") Object getFoo() {
    return foo;
  }

  Object @GuardedBy("lock") [] getFooArray() {
    return fooArray;
  }

  @GuardedBy("lock") Object[] getFooArray2() {
    return fooArray2;
  }

  @GuardedBy("lock") Object[][] getFooArray3() {
    return fooArray3;
  }

  Object @GuardedBy("lock") [][] getFooArray4() {
    return fooArray4;
  }

  Object[] @GuardedBy("lock") [] getFooArray5() {
    return fooArray5;
  }

  enum myEnumType {
    ABC, DEF
  }

  @GuardedBy("lock")
  myEnumType myEnum;

public void testOperationsWithPrimitives() {
    int i = 0;
    boolean b;

    //:: error: (contracts.precondition.not.satisfied.field)
    i = i >>> primitive;
    //:: error: (contracts.precondition.not.satisfied.field)
    i = primitive >>> i;

    //:: error: (contracts.precondition.not.satisfied.field)
    i >>>= primitive;
    //:: error: (contracts.precondition.not.satisfied.field)
    primitive >>>= i;

    //:: error: (contracts.precondition.not.satisfied.field)
    i %= primitive;
    //:: error: (contracts.precondition.not.satisfied.field)
    i = 4 % primitive;
    //:: error: (contracts.precondition.not.satisfied.field)
    i = primitive % 4;

    //:: error: (contracts.precondition.not.satisfied.field)
    primitive++;
    //:: error: (contracts.precondition.not.satisfied.field)
    primitive--;
    //:: error: (contracts.precondition.not.satisfied.field)
    ++primitive;
    //:: error: (contracts.precondition.not.satisfied.field)
    --primitive;

    //:: error: (contracts.precondition.not.satisfied.field)
    if (primitive != 5){ }

    //:: error: (contracts.precondition.not.satisfied.field)
    i = primitive >> i;
    //:: error: (contracts.precondition.not.satisfied.field)
    i = primitive << i;
    //:: error: (contracts.precondition.not.satisfied.field)
    i = i >> primitive;
    //:: error: (contracts.precondition.not.satisfied.field)
    i = i << primitive;

    //:: error: (contracts.precondition.not.satisfied.field)
    i <<= primitive;
    //:: error: (contracts.precondition.not.satisfied.field)
    i >>= primitive;
    //:: error: (contracts.precondition.not.satisfied.field)
    primitive <<= i;
    //:: error: (contracts.precondition.not.satisfied.field)
    primitive >>= i;

    //:: error: (contracts.precondition.not.satisfied.field)
    assert(primitiveBoolean);

    //:: error: (contracts.precondition.not.satisfied.field)
    b = primitive >= i;
    //:: error: (contracts.precondition.not.satisfied.field)
    b = primitive <= i;
    //:: error: (contracts.precondition.not.satisfied.field)
    b = primitive > i;
    //:: error: (contracts.precondition.not.satisfied.field)
    b = primitive < i;
    //:: error: (contracts.precondition.not.satisfied.field)
    b = i >= primitive;
    //:: error: (contracts.precondition.not.satisfied.field)
    b = i <= primitive;
    //:: error: (contracts.precondition.not.satisfied.field)
    b = i > primitive;
    //:: error: (contracts.precondition.not.satisfied.field)
    b = i < primitive;

    //:: error: (contracts.precondition.not.satisfied.field)
    i += primitive;
    //:: error: (contracts.precondition.not.satisfied.field)
    i -= primitive;
    //:: error: (contracts.precondition.not.satisfied.field)
    i *= primitive;
    //:: error: (contracts.precondition.not.satisfied.field)
    i /= primitive;

    //:: error: (contracts.precondition.not.satisfied.field)
    i = 4 + primitive;
    //:: error: (contracts.precondition.not.satisfied.field)
    i = 4 - primitive;
    //:: error: (contracts.precondition.not.satisfied.field)
    i = 4 * primitive;
    //:: error: (contracts.precondition.not.satisfied.field)
    i = 4 / primitive;

    //:: error: (contracts.precondition.not.satisfied.field)
    i = primitive + 4;
    //:: error: (contracts.precondition.not.satisfied.field)
    i = primitive - 4;
    //:: error: (contracts.precondition.not.satisfied.field)
    i = primitive * 4;
    //:: error: (contracts.precondition.not.satisfied.field)
    i = primitive / 4;




    //:: error: (contracts.precondition.not.satisfied.field)
    if (primitiveBoolean){}

    //:: error: (contracts.precondition.not.satisfied.field)
    i = ~primitive;

    //:: error: (contracts.precondition.not.satisfied.field)
    b = primitiveBoolean || false;
    //:: error: (contracts.precondition.not.satisfied.field)
    b = primitiveBoolean | false;

    //:: error: (contracts.precondition.not.satisfied.field)
    b = primitiveBoolean ^ true;

    //:: error: (contracts.precondition.not.satisfied.field)
    b &= primitiveBoolean;

    //:: error: (contracts.precondition.not.satisfied.field)
    b |= primitiveBoolean;

    //:: error: (contracts.precondition.not.satisfied.field)
    b ^= primitiveBoolean;

    //:: error: (contracts.precondition.not.satisfied.field)
    b = !primitiveBoolean;


    //:: error: (contracts.precondition.not.satisfied.field)
    i = primitive;


    //:: error: (contracts.precondition.not.satisfied.field)
    b = true && primitiveBoolean;
    //:: error: (contracts.precondition.not.satisfied.field)
    b = true & primitiveBoolean;

    //:: error: (contracts.precondition.not.satisfied.field)
    b = false || primitiveBoolean;
    //:: error: (contracts.precondition.not.satisfied.field)
    b = false | primitiveBoolean;

    //:: error: (contracts.precondition.not.satisfied.field)
    b = false ^ primitiveBoolean;

    //:: error: (contracts.precondition.not.satisfied.field)
    b = primitiveBoolean && true;
    //:: error: (contracts.precondition.not.satisfied.field)
    b = primitiveBoolean & true;
}

void testEnumType() {
    //:: error: (assignment.type.incompatible)
    myEnum = myEnumType.ABC; // TODO: assignment.type.incompatible is technically correct, but we could make it friendlier for the user if constant enum values on the RHS
                             // automatically cast to the @GuardedBy annotation of the LHS
}

void testTreeTypes() {
    int i;

    Object o = new Object();
    Object f = new Object();

    // The following test cases were inspired from annotator.find.ASTPathCriterion.isSatisfiedBy in the Annotation File Utilities

// Hits a bug in dataflow:    do { break; } while(foo != null); // access to guarded object in while condition of do/while loop
// Hits a bug in dataflow:    for(foo = new Object(); foo != null; foo = new Object()){ break; } // access to guarded object in condition of for loop
    foo = new Object(); // assignment to guarded object (OK)
    // TODO: Make the synchronized expression count as a dereference even if it is not a MemberSelectTree --- :: error: (contracts.precondition.not.satisfied.field)
    synchronized(foo){ // attempt to use guarded object as a lock - this counts as a dereference of foo
    }
    //:: error: (contracts.precondition.not.satisfied.field)
    switch(foo.hashCode()){ // attempt to use guarded object in a switch statement
    }
    // try(foo = new Object()){ foo.toString(); } // attempt to use guarded object inside a try with resources

    //:: error: (contracts.precondition.not.satisfied.field)
    fooArray[0].toString(); // method call on dereference of unguarded element of *guarded* array
    //:: error: (contracts.precondition.not.satisfied.field)
    fooArray.toString(); // method call on dereference of guarded array itself

    //:: error: (contracts.precondition.not.satisfied.field)
    fooArray2[0].toString(); // method call on dereference of guarded array element
    // fooArray2.toString(); // method call on dereference of unguarded array - TODO: currently preconditions are not retrieved correctly from array types. This is not unique to the Lock Checker.

    //:: error: (contracts.precondition.not.satisfied.field)
    fooArray3[0][0].toString(); // method call on dereference of guarded array element of multidimensional array
    // fooArray3[0].toString(); // method call on dereference of unguarded single-dimensional array element of unguarded multidimensional array - TODO: currently preconditions are not retrieved correctly from array types. This is not unique to the Lock Checker.
    // fooArray3.toString(); // method call on dereference of unguarded multidimensional array - TODO: currently preconditions are not retrieved correctly from array types. This is not unique to the Lock Checker.

    //:: error: (contracts.precondition.not.satisfied.field)
    fooArray4[0][0].toString(); // method call on dereference of unguarded array element of *guarded* multidimensional array
    //:: error: (contracts.precondition.not.satisfied.field)
    fooArray4[0].toString(); // method call on dereference of unguarded single-dimensional array element of *guarded* multidimensional array
    //:: error: (contracts.precondition.not.satisfied.field)
    fooArray4.toString(); // method call on dereference of guarded multidimensional array

    //:: error: (contracts.precondition.not.satisfied.field)
    fooArray5[0][0].toString(); // method call on dereference of unguarded array element of *guarded subarray* of multidimensional array
    //:: error: (contracts.precondition.not.satisfied.field)
    fooArray5[0].toString(); // method call on dereference of guarded single-dimensional array element of multidimensional array
    fooArray5.toString(); // method call on dereference of unguarded multidimensional array

    //:: error: (contracts.precondition.not.satisfied)
    getFooArray().toString(); // method call on dereference of guarded array returned by a method

    //:: error: (contracts.precondition.not.satisfied.field)
    getFooArray2()[0].toString(); // method call on dereference of guarded array element returned by a method
    getFooArray2().toString(); // method call on dereference of unguarded array returned by a method

    //:: error: (contracts.precondition.not.satisfied.field)
    getFooArray3()[0][0].toString(); // method call on dereference of guarded array element of multidimensional array returned by a method
    getFooArray3()[0].toString(); // method call on dereference of unguarded single-dimensional array element of multidimensional array returned by a method
    getFooArray3().toString(); // method call on dereference of unguarded multidimensional array returned by a method

    //:: error: (contracts.precondition.not.satisfied)
    getFooArray4()[0][0].toString(); // method call on dereference of unguarded array element of *guarded* multidimensional array returned by a method
    //:: error: (contracts.precondition.not.satisfied)
    getFooArray4()[0].toString(); // method call on dereference of unguarded single-dimensional array element of *guarded* multidimensional array returned by a method
    //:: error: (contracts.precondition.not.satisfied)
    getFooArray4().toString(); // method call on dereference of guarded multidimensional array returned by a method

    //:: error: (contracts.precondition.not.satisfied.field)
    getFooArray5()[0][0].toString(); // method call on dereference of unguarded array element of *guarded subarray* of multidimensional array returned by a method
    //:: error: (contracts.precondition.not.satisfied.field)
    getFooArray5()[0].toString(); // method call on dereference of guarded single-dimensional array element of multidimensional array returned by a method
    getFooArray5().toString(); // method call on dereference of unguarded multidimensional array returned by a method

    //:: error: (contracts.precondition.not.satisfied.field)
    String s = (foo.toString()); // method call on guarded object within parenthesized expression
    //:: error: (contracts.precondition.not.satisfied.field)
    foo.toString(); // method call on guarded object
    //:: error: (contracts.precondition.not.satisfied)
    getFoo().toString(); // method call on guarded object returned by a method
    //:: error: (contracts.precondition.not.satisfied.field)
    this.foo.toString(); // method call on guarded object using 'this' literal
    //:: error: (contracts.precondition.not.satisfied.field)
    label: foo.toString(); // dereference of guarded object in labeled statement
    if (foo instanceof Object){} // access to guarded object in instanceof expression (OK)
    while(foo != null){ break; } // access to guarded object in while condition of while loop (OK)
    if (false){}else if (foo == o){} // binary operator on guarded object in else if condition (OK)
    // Hits a bug in dataflow:    Runnable rn = () -> { foo.toString(); }; // access to guarded object in a lambda expression
    //:: error: (contracts.precondition.not.satisfied.field)
    i = myClassInstance.i; // access to member field of guarded object
    // MemberReferenceTrees? how do they work
    fooArray = new Object[3]; // second allocation of guarded array (OK)
    //:: error: (contracts.precondition.not.satisfied.field)
    s = i == 5 ? foo.toString() : f.toString(); // dereference of guarded object in conditional expression tree
    //:: error: (contracts.precondition.not.satisfied.field)
    s = i == 5 ? f.toString() : foo.toString(); // dereference of guarded object in conditional expression tree
    // Testing of 'return' is done in getFooWithWrongReturnType()
    //:: error: (throw.type.invalid)
    try{ throw exception; } catch(Exception e){} // throwing a guarded object - when throwing an exception, it must be @GuardedBy({}). Even @GuardedByInaccessible is not allowed.
    //:: error: (assignment.type.incompatible)
    Object e = (Object) exception; // casting of a guarded object
    //:: error: (contracts.precondition.not.satisfied.field)
    myList.clear(); // dereference of guarded object having a parameterized type

    // We need to support locking on local variables and protecting local variables because these locals may contain references to fields. Somehow we need to pass along the information of which field it was.

    if (foo == o) { // binary operator on guarded object (OK)
      o.toString();
    }
    //
    if (foo == null) {
      //:: error: (contracts.precondition.not.satisfied.field)
      foo.toString();
    }
}

public void testLocals() {
  ReentrantLock localLock = new ReentrantLock();

  @GuardedBy("localLock")
  Object guardedByLocalLock = new Object();

  //:: error: (contracts.precondition.not.satisfied.field)
  guardedByLocalLock.toString();

  @GuardedBy("lock")
  Object local = new Object();

  //:: error: (contracts.precondition.not.satisfied.field)
  local.toString();

  lockTheLock();

  local.toString(); // No warning output

  unlockTheLock();
}

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
    foo.toString();

    unlockTheLock();

    //:: error: (assignment.type.incompatible)
    @LockHeld ReentrantLock heldLock2 = lock;
    //:: error: (contracts.precondition.not.satisfied.field)
    foo.toString();
  }
  else {
    //:: error: (assignment.type.incompatible)
    @LockHeld ReentrantLock heldLock = lock;
    //:: error: (contracts.precondition.not.satisfied.field)
    foo.toString();
  }

  if (tryToLockTheLock()) {
    @LockHeld ReentrantLock heldLock = lock;
    foo.toString();
  }
  else {
    //:: error: (assignment.type.incompatible)
    @LockHeld ReentrantLock heldLock = lock;
    //:: error: (contracts.precondition.not.satisfied.field)
    foo.toString();
  }

  if (r.nextBoolean()) {
    lockTheLock();
    sideEffectFreeMethod();
    @LockHeld ReentrantLock heldLock = lock;
    foo.toString();
  }
  else {
    lockTheLock();
    nonSideEffectFreeMethod();
    //:: error: (assignment.type.incompatible)
    @LockHeld ReentrantLock heldLock = lock;
    //:: error: (contracts.precondition.not.satisfied.field)
    foo.toString();
  }

  if (r.nextBoolean()) {
    lockTheLock();
    lockingFreeMethod();
    @LockHeld ReentrantLock heldLock = lock;
    foo.toString();
  }
  else {
    lockTheLock();
    nonSideEffectFreeMethod();
    //:: error: (assignment.type.incompatible)
    @LockHeld ReentrantLock heldLock = lock;
    //:: error: (contracts.precondition.not.satisfied.field)
    foo.toString();
  }
}

}
