// This test contains the sample code from the Lock Checker manual chapter
// modified to fit testing instead of illustrative purposes.
import org.checkerframework.checker.lock.qual.*;
import org.checkerframework.dataflow.qual.*;
import java.util.concurrent.locks.ReentrantLock;

class ChapterExamples {
  class MyClass {
     Object field = new Object();
     @LockingFree
     Object method(@GuardSatisfied MyClass this){return new Object();}
  }

@MayReleaseLocks
@ReleasesNoLocks
// TODO: enable (multiple.sideeffect.annotation)
void testMultipleSideEffectAnnotations(){
}

void guardedByItselfOnReceiver(@GuardedBy("itself") ChapterExamples this) {
  synchronized(this) { // Tests translation of 'itself' to 'this' by the LockVisitor for this scenario.
    //myField = new MyClass();
    myField.toString();
    this.myField = new MyClass();
    this.myField.toString();
  }
  //:: error: (contracts.precondition.not.satisfied.field)
  myField = new MyClass();
  //:: error: (contracts.precondition.not.satisfied.field)
  myField.toString();
  //:: error: (contracts.precondition.not.satisfied.field)
  this.myField = new MyClass();
  //:: error: (contracts.precondition.not.satisfied.field)
  this.myField.toString();
}

void guardedByThisOnReceiver(@GuardedBy("this") ChapterExamples this) {
  //:: error: (contracts.precondition.not.satisfied.field)
  myField = new MyClass();
  //:: error: (contracts.precondition.not.satisfied.field)
  myField.toString();
  //:: error: (contracts.precondition.not.satisfied.field)
  this.myField = new MyClass();
  //:: error: (contracts.precondition.not.satisfied.field)
  this.myField.toString();
  synchronized(this) {
    myField = new MyClass();
    myField.toString();
    this.myField = new MyClass();
    this.myField.toString();
  }
}

void testDereferenceOfReceiverAndParameter(@GuardedBy("lock") ChapterExamples this, @GuardedBy("lock") MyClass m) {
  //:: error: (contracts.precondition.not.satisfied.field)
  myField = new MyClass();
  //:: error: (contracts.precondition.not.satisfied.field)
  myField.toString();
  //:: error: (contracts.precondition.not.satisfied.field)
  this.myField = new MyClass();
  //:: error: (contracts.precondition.not.satisfied.field)
  this.myField.toString();
  //:: error: (contracts.precondition.not.satisfied.field)
  m.field = new Object();
  //:: error: (contracts.precondition.not.satisfied.field)
  m.field.toString();
  synchronized(lock) {
    myField = new MyClass();
    myField.toString();
    this.myField = new MyClass();
    this.myField.toString();
    m.field = new Object();
    m.field.toString();
  }
}

  @GuardedBy("lock") MyClass myObj = new MyClass();

  @LockingFree
  @GuardedBy("lock") MyClass myMethodReturningMyObj() { return myObj; }

  ChapterExamples() {
    lock = new Object();
  }

  void myMethod8(){
    //:: error: (contracts.precondition.not.satisfied)
    boolean b4 = compare(p1, myMethod());


    //:: error: (contracts.precondition.not.satisfied.field)
    boolean b2 = compare(p1, p2); // An error is issued indicating that p2 might be dereferenced without "lock" being held. The method call need not be modified, since @GuardedBy({}) <: @GuardedByInaccessible and @GuardedBy("lock") <: @GuardedByInaccessible, but the lock must be acquired prior to the method call.
    //:: error: (contracts.precondition.not.satisfied.field)
    boolean b3 = compare(p1, this.p2);
    //:: error: (contracts.precondition.not.satisfied)
    boolean b5 = compare(p1, this.myMethod());
    synchronized(lock){
      boolean b6 = compare(p1, p2); // OK
      boolean b7 = compare(p1, this.p2); // OK
      boolean b8 = compare(p1, myMethod()); // OK
      boolean b9 = compare(p1, this.myMethod()); // OK
    }
  }

  // Keep in mind, the expression itself may or may not be a
  // method call. Simple examples of expression.identifier :
  // myObject.field
  // myMethod().field
  // myObject.method()
  // myMethod().method()

void myMethod7(){
  //:: error: (contracts.precondition.not.satisfied.field)
  Object f = myObj.field;
  //:: error: (contracts.precondition.not.satisfied)
  Object f2 = myMethodReturningMyObj().field;
  //:: error: (contracts.precondition.not.satisfied)
  myObj.method(); // method()'s receiver is annotated as @GuardSatisfied
  //:: error: (contracts.precondition.not.satisfied)
  myMethodReturningMyObj().method(); // method()'s receiver is annotated as @GuardSatisfied
  // TODO: test a call to 'method()' with the receiver being the current object.

  synchronized(lock){
    f = myObj.field;
    f2 = myMethodReturningMyObj().field;
    myObj.method();
    myMethodReturningMyObj().method();
  }


  //:: error: (contracts.precondition.not.satisfied)
  myMethodReturningMyObj().field = new Object();
  //:: error: (contracts.precondition.not.satisfied.field)
  x.field = new Object();
  synchronized(lock){
    myMethod().field = new Object();
  }
  synchronized(lock){
    x.field = new Object(); // toString is not LockingFree. How annoying.
  }

  this.x = new MyClass();
}


  Object lock; // Initialized in the constructor

  @GuardedBy("lock") MyClass x = new MyClass();
  @GuardedBy("lock") MyClass y = x; // OK, because dereferences of y will require "lock" to be held.
  //:: error: (assignment.type.incompatible)
  @GuardedBy({}) MyClass z = x; // ILLEGAL because dereferences of z do not require "lock" to be held.
  @LockingFree
  @GuardedBy("lock") MyClass myMethod(){
     return x; // OK because the return type is @GuardedBy("lock")
  }

  void exampleMethod(){
     //:: error: (contracts.precondition.not.satisfied.field)
     x.field = new Object(); // ILLEGAL because the lock is not known to be held
     //:: error: (contracts.precondition.not.satisfied.field)
     y.field = new Object(); // ILLEGAL because the lock is not known to be held
     //:: error: (contracts.precondition.not.satisfied)
     myMethod().field = new Object(); // ILLEGAL because the lock is not known to be held
     synchronized(lock) {
       x.field = new Object();  // OK: the lock is known to be held
       y.field = new Object();  // OK: the lock is known to be held
       myMethod().field = new Object(); // OK: the lock is known to be held
     }
  }

    MyClass a = new MyClass();
    MyClass b = new MyClass();
    @GuardedBy("a") MyClass x5 = new MyClass();
    @GuardedBy({"a", "b"}) MyClass y5 = new MyClass();
    void myMethod2() {
        //:: error: (assignment.type.incompatible)
        y5 = x5; // ILLEGAL
    }

    @GuardedBy("a") String s = "string"; // OK


    @GuardedBy({}) MyClass o1;
    @GuardedBy("lock") MyClass o2;
    @GuardedBy("lock") MyClass o3;

    void someMethod() {
      o3 = o2; // OK, since o2 and o3 are guarded by exactly the same lock set.

      //:: error: (assignment.type.incompatible)
      o1 = o2; // Assignment type incompatible errors are issued for both assignments, since
      //:: error: (assignment.type.incompatible)
      o2 = o1; // {"lock"} and {} are not identical sets.
    }

    @SuppressWarnings("lock:cast.unsafe")
    void someMethod2() {
       o1 = (@GuardedBy({}) MyClass) o2; // A cast can be used if the user knows it is safe to do so. However the @SuppressWarnings must be added.
    }

    static Object myLock = new Object();

 @GuardedBy("ChapterExamples.myLock") MyClass myMethod3() { return new MyClass(); }

  // reassignments without holding the lock are OK.
  @GuardedBy("ChapterExamples.myLock") MyClass x2 = myMethod3();
  @GuardedBy("ChapterExamples.myLock") MyClass y2 = x2;

  void myMethod4() {
    //:: error: (contracts.precondition.not.satisfied.field)
    x2.field = new Object(); // ILLEGAL because the lock is not held
    synchronized(ChapterExamples.myLock) {
      y2.field = new Object();  // OK: the lock is held
    }
  }

 void myMethod5(@GuardedBy("ChapterExamples.myLock") MyClass a) {
    //:: error: (contracts.precondition.not.satisfied.field)
    a.field = new Object(); // ILLEGAL: the lock is not held
    synchronized(ChapterExamples.myLock) {
      a.field = new Object();  // OK: the lock is held
    }
  }


@LockingFree
boolean compare(@GuardSatisfied MyClass a, @GuardSatisfied MyClass b){ return true; }

@GuardedBy({}) MyClass p1;
@GuardedBy("lock") MyClass p2;

void myMethod6(){
  synchronized(lock){ // It is the responsibility of callers to 'compare' to acquire the lock.
      boolean b1 = compare(p1, p2); // OK. No error issued.
  }
  //:: error: (contracts.precondition.not.satisfied.field)
  p2.field = new Object();
  //:: error: (contracts.precondition.not.satisfied.field)
  boolean b2 = compare(p1, p2); // An error is issued indicating that p2 might be dereferenced without "lock" being held. The method call need not be modified, since @GuardedBy({}) <: @GuardedByInaccessible and @GuardedBy("lock") <: @GuardedByInaccessible, but the lock must be acquired prior to the method call.
}

 void helper1(@GuardedBy("ChapterExamples.myLock") MyClass a) {
    //:: error: (contracts.precondition.not.satisfied.field)
    a.field = new Object(); // ILLEGAL: the lock is not held
    synchronized(ChapterExamples.myLock) {
      a.field = new Object();  // OK: the lock is held
    }
  }
  @Holding("ChapterExamples.myLock")
  @LockingFree
  void helper2(@GuardedBy("ChapterExamples.myLock") MyClass b) {
    b.field = new Object(); // OK: the lock is held
  }
  @LockingFree
  void helper3(@GuardSatisfied MyClass c) {
    c.field = new Object(); // OK: the guard is satisfied
  }
  @LockingFree
  void helper4(@GuardedBy("ChapterExamples.myLock") MyClass d) {
    //:: error: (contracts.precondition.not.satisfied.field)
    d.field = new Object(); // ILLEGAL: the lock is not held
  }
  @ReleasesNoLocks
  void helper5() { }
  // No annotation means @ReleasesNoLocks
  void helper6() { }
  void myMethod2(@GuardedBy("ChapterExamples.myLock") MyClass e) {
    helper1(e);  // OK to pass to another routine without holding the lock.
    //:: error: (contracts.precondition.not.satisfied.field)
    e.field = new Object(); // ILLEGAL: the lock is not held
    //:: error: (contracts.precondition.not.satisfied)
    helper2(e);
    //:: error: (contracts.precondition.not.satisfied.field)
    helper3(e);
    synchronized (ChapterExamples.myLock) {
      helper2(e);
      helper3(e); // OK, since parameter is @GuardSatisfied
      helper4(e); // OK, but helper4's body still has an error.
      helper5();
      helper6();
      helper2(e); // Can still be called after helper5() and helper6()
    }
  }

private @GuardedBy({}) MyClass myField;
private ReentrantLock myLock2; // Initialized in the constructor
private @GuardedBy("myLock2") MyClass x3; // Initialized in the constructor

// This method does not use locks or synchronization but cannot
// be annotated as @SideEffectFree since it alters myField.
@LockingFree
void myMethod5() {
    myField = new MyClass();
}

@SideEffectFree
int mySideEffectFreeMethod() {
    return 0;
}

@MayReleaseLocks
void myUnlockingMethod() {
    myLock2.unlock();
}

@MayReleaseLocks
void myReleaseLocksEmptyMethod() {
}

@MayReleaseLocks
//:: error: (guardsatisfied.with.mayreleaselocks)
void methodGuardSatisfiedReceiver(@GuardSatisfied ChapterExamples this) {
}

@MayReleaseLocks
//:: error: (guardsatisfied.with.mayreleaselocks)
void methodGuardSatisfiedParameter(@GuardSatisfied Object o) {
}

@MayReleaseLocks
void myOtherMethod() {
    if (myLock2.tryLock()) {
        x3.field = new Object(); // OK: the lock is held
        myMethod5();
        x3.field = new Object(); // OK: the lock is still known to be held since myMethod is locking-free
        mySideEffectFreeMethod();
        x3.field = new Object(); // OK: the lock is still known to be held since mySideEffectFreeMethod
                      // is side-effect-free
        myUnlockingMethod();
        //:: error: (contracts.precondition.not.satisfied.field)
        x3.field = new Object(); // ILLEGAL: myLockingMethod is not locking-free
    }
    if (myLock2.tryLock()) {
        x3.field = new Object(); // OK: the lock is held
        myReleaseLocksEmptyMethod();
        //:: error: (contracts.precondition.not.satisfied.field)
        x3.field = new Object(); // ILLEGAL: even though myUnannotatedEmptyMethod is empty, since
                      // myReleaseLocksEmptyMethod() is annotated with @MayReleaseLocks and the Lock Checker no longer knows
                      // the state of the lock.
        if (myLock2.isHeldByCurrentThread()) {
            x3.field = new Object(); // OK: the lock is known to be held
        }
    }
}

void unboxing() {
    int a = 1;
    @GuardedBy("lock") Integer c;
    synchronized(lock) {
        c = a;
    }

    @GuardedBy("lock") Integer b = 1;
    int d;
    synchronized(lock) {
        d = b;
        d = b.intValue(); // The de-sugared version does not issue an error.
    }

    //:: error: (contracts.precondition.not.satisfied.field)
    c = c + b; // Syntactic sugar for c = new Integer(c.intValue() + b.intValue()), hence 'lock' must be held.
    //:: error: (contracts.precondition.not.satisfied.field)
    c = new Integer(c.intValue() + b.intValue()); // The de-sugared version

    synchronized(lock) {
        c = c + b; // Syntactic sugar for c = new Integer(c.intValue() + b.intValue()), hence 'lock' must be held.
        c = new Integer(c.intValue() + b.intValue()); // The de-sugared version
    }

    //:: error: (contracts.precondition.not.satisfied.field)
    a = b; // TODO: This assignment between two reference types should not require a lock to be held. See the explanation in LockVisitor.checkAccess for more information.
    b = c; // OK
}

/* TODO Re-enable when guarding primitives is supported by the Lock Checker.
void boxingUnboxing() {
    @GuardedBy("lock") int a = 1;
    @GuardedBy({}) Integer c;
    synchronized(lock) {
        c = a;
    }

    @GuardedBy("lock") Integer b = 1;
    @GuardedBy({}) int d;
    synchronized(lock) {
        // TODO re-enable this error (assignment.type.incompatible)
        d = b; // TODO: This should not result in assignment.type.incompatible because 'b' is actually syntactic sugar for b.intValue(). See the explanation in LockVisitor.checkAccess for more information.
        d = b.intValue(); // The de-sugared version does not issue an error.
    }

    // TODO re-enable this error (contracts.precondition.not.satisfied.field)
    c = c + b; // Syntactic sugar for c = new Integer(c.intValue() + b.intValue()), hence 'lock' must be held.
    // TODO re-enable this error (contracts.precondition.not.satisfied.field)
    c = new Integer(c.intValue() + b.intValue()); // The de-sugared version

    synchronized(lock) {
        c = c + b; // Syntactic sugar for c = new Integer(c.intValue() + b.intValue()), hence 'lock' must be held.
        c = new Integer(c.intValue() + b.intValue()); // The de-sugared version
    }

    // TODO re-enable this error (contracts.precondition.not.satisfied.field)
    a = b; // TODO: This assignment between two reference types should not require a lock to be held. See the explanation in LockVisitor.checkAccess for more information.
}*/


ReentrantLock lock1, lock2;
@GuardedBy("lock1") StringBuffer filename;
@GuardedBy("lock2") StringBuffer extension;

void method0() {
    //:: error: (contracts.precondition.not.satisfied) :: error: (contracts.precondition.not.satisfied.field)
    filename.append(extension);
    // filename = filename.append(extension);
}

void method1() {
    lock1.lock();
    //:: error: (contracts.precondition.not.satisfied.field)
    filename.append(extension);
    // filename = filename.append(extension);
}

void method2() {
    lock2.lock();
    //:: error: (contracts.precondition.not.satisfied)
    filename.append(extension);
    // filename = filename.append(extension);
}

void method3() {
    lock1.lock();
    lock2.lock();
    filename.append(extension);
    // filename = filename.append(extension);
}

@ReleasesNoLocks
void innerClassTest() {
   class InnerClass {
       @MayReleaseLocks
       void innerClassMethod() {
       }
   }

   InnerClass ic = new InnerClass();
   //:: error: (method.guarantee.violated)
   ic.innerClassMethod();
}

//@LockingFree
//public @GuardSatisfied(1) StringBuffer append(@GuardSatisfied(1) StringBuffer this,
//                                              @GuardSatisfied(2) String str)


}
