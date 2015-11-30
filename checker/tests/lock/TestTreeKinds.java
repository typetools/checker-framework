import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.locks.ReentrantLock;
import org.checkerframework.checker.lock.qual.*;
import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.dataflow.qual.LockingFree;

public class TestTreeKinds {
  ReentrantLock lock = new ReentrantLock();

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

  @GuardedBy("lock")
  Object fooArray[] = new Object[3];

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

  Object getFoo() {
    //:: error: (contracts.precondition.not.satisfied.field)
    return foo; // return of guarded object
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
    if (primitive != 5) { }

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
    myEnum = myEnumType.ABC;

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
    if (primitiveBoolean) {}

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

void testTreeTypes() {
    int i;

    Object o = new Object();
    Object f = new Object();

    // The following test cases were inspired from annotator.find.ASTPathCriterion.isSatisfiedBy in the Annotation File Utilities

// Hits a bug in dataflow:    do { break; } while (foo != null); // access to guarded object in while condition of do/while loop
// Hits a bug in dataflow:    for(foo = new Object(); foo != null; foo = new Object()) { break; } // access to guarded object in condition of for loop
    //:: error: (contracts.precondition.not.satisfied.field)
    foo = new Object(); // assignment to guarded object
    //:: error: (contracts.precondition.not.satisfied.field)
    synchronized(foo) { // attempt to use guarded object as a lock
    }
    //:: error: (contracts.precondition.not.satisfied.field)
    switch(foo.hashCode()) { // attempt to use guarded object in a switch statement
    }
    // try(foo = new Object()) { foo.toString(); } // attempt to use guarded object inside a try with resources
    //:: error: (contracts.precondition.not.satisfied.field)
    fooArray[0].toString(); // method call on access of guarded array
    //:: error: (contracts.precondition.not.satisfied.field)
    String s = (foo.toString()); // method call on guarded object within parenthesized expression
    //:: error: (contracts.precondition.not.satisfied.field)
    foo.toString(); // method call on guarded object
    //:: error: (contracts.precondition.not.satisfied.field)
    this.foo.toString(); // method call on guarded object using 'this' literal
    //:: error: (contracts.precondition.not.satisfied.field)
    label: foo.toString(); // access to guarded object in labeled statement
    //:: error: (contracts.precondition.not.satisfied.field)
    if (foo instanceof Object) {} // access to guarded object in instanceof expression
    //:: error: (contracts.precondition.not.satisfied.field)
    while (foo != null) { break; } // access to guarded object in while condition of while loop
    //:: error: (contracts.precondition.not.satisfied.field)
    if (false) {} else if (foo == o) {} // binary operator on guarded object in else if condition
    // Hits a bug in dataflow:    Runnable rn = () -> { foo.toString(); }; // access to guarded object in a lambda expression
    //:: error: (contracts.precondition.not.satisfied.field)
    i = myClassInstance.i; // access to member field of guarded object
    // MemberReferenceTrees? how do they work
    //:: error: (contracts.precondition.not.satisfied.field)
    fooArray = new Object[3]; // second allocation of guarded array
    //:: error: (contracts.precondition.not.satisfied.field)
    s = i == 5 ? foo.toString() : f.toString(); // access to guarded object in conditional expression tree
    //:: error: (contracts.precondition.not.satisfied.field)
    s = i == 5 ? f.toString() : foo.toString(); // access to guarded object in conditional expression tree
    // Testing of 'return' is done in getFoo()
    //:: error: (contracts.precondition.not.satisfied.field)
    try { throw exception; } catch(Exception e) {} // throwing a guarded object
    //:: error: (contracts.precondition.not.satisfied.field)
    Object e = (Object) exception; // casting of a guarded object
    //:: error: (contracts.precondition.not.satisfied.field)
    myList.clear(); // access to guarded object having a parameterized type

    // We need to support locking on local variables and protecting local variables because these locals may contain references to fields. Somehow we need to pass along the information of which field it was.

    //:: error: (contracts.precondition.not.satisfied.field)
    if (foo == o) { // binary operator on guarded object
      o.toString();
    }
    //:: error: (contracts.precondition.not.satisfied.field)
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
    foo = new Object();

    unlockTheLock();

    //:: error: (assignment.type.incompatible)
    @LockHeld ReentrantLock heldLock2 = lock;
    //:: error: (contracts.precondition.not.satisfied.field)
    foo = new Object();
  }
  else {
    //:: error: (assignment.type.incompatible)
    @LockHeld ReentrantLock heldLock = lock;
    //:: error: (contracts.precondition.not.satisfied.field)
    foo = new Object();
  }

  if (tryToLockTheLock()) {
    @LockHeld ReentrantLock heldLock = lock;
    foo = new Object();
  }
  else {
    //:: error: (assignment.type.incompatible)
    @LockHeld ReentrantLock heldLock = lock;
    //:: error: (contracts.precondition.not.satisfied.field)
    foo = new Object();
  }

  if (r.nextBoolean()) {
    lockTheLock();
    sideEffectFreeMethod();
    @LockHeld ReentrantLock heldLock = lock;
    foo = new Object();
  }
  else {
    lockTheLock();
    nonSideEffectFreeMethod();
    //:: error: (assignment.type.incompatible)
    @LockHeld ReentrantLock heldLock = lock;
    //:: error: (contracts.precondition.not.satisfied.field)
    foo = new Object();
  }

  if (r.nextBoolean()) {
    lockTheLock();
    lockingFreeMethod();
    @LockHeld ReentrantLock heldLock = lock;
    foo = new Object();
  }
  else {
    lockTheLock();
    nonSideEffectFreeMethod();
    //:: error: (assignment.type.incompatible)
    @LockHeld ReentrantLock heldLock = lock;
    //:: error: (contracts.precondition.not.satisfied.field)
    foo = new Object();
  }
}

}
