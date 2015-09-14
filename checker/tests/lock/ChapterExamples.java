// This test contains the sample code from the Lock Checker manual chapter
// slightly modified to fit testing instead of illustrative purposes.
import org.checkerframework.checker.lock.qual.*;
import org.checkerframework.dataflow.qual.*;
import java.util.concurrent.locks.ReentrantLock;

class ChapterExamples {
  class MyInnerClass {
     Object field = new Object();
     @LockingFree
     Object method(){return new Object();}
  }

  @GuardedBy("lock") MyInnerClass myObj = new MyInnerClass();

  @LockingFree
  @GuardedBy("lock") MyInnerClass myMethodReturningMyObj() { return myObj; }

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
  //:: error: (contracts.precondition.not.satisfied.field)
  myObj.method();
  // TODO: test a call to 'method()' with the receiver being the current object.
  //:: error: (contracts.precondition.not.satisfied)
  myMethodReturningMyObj().method();

  synchronized(lock){
    f = myObj.field;
    f2 = myMethodReturningMyObj().field;
    myObj.method();
    myMethodReturningMyObj().method();
  }


  //:: error: (contracts.precondition.not.satisfied)
  myMethodReturningMyObj().toString();
  //:: error: (contracts.precondition.not.satisfied.field)
  x.toString();
  synchronized(lock){
    myMethod().toString();
  }
  synchronized(lock){
    x.toString(); // toString is not LockingFree. How annoying.
  }

  this.x = new Object();
}


  Object lock; // Initialized in the constructor

  @GuardedBy("lock") Object x = new Object();
  @GuardedBy("lock") Object y = x; // OK, because dereferences of y will require "lock" to be held.
  //:: error: (assignment.type.incompatible)
  @GuardedBy({}) Object z = x; // ILLEGAL because dereferences of z do not require "lock" to be held.
  @LockingFree
  @GuardedBy("lock") Object myMethod(){
     return x; // OK because the return type is @GuardedBy("lock")
  }

  void exampleMethod(){
     //:: error: (contracts.precondition.not.satisfied.field)
     x.toString(); // ILLEGAL because the lock is not known to be held
     //:: error: (contracts.precondition.not.satisfied.field)
     y.toString(); // ILLEGAL because the lock is not known to be held
     //:: error: (contracts.precondition.not.satisfied)
     myMethod().toString(); // ILLEGAL because the lock is not known to be held
     synchronized(lock) {
       x.toString();  // OK: the lock is known to be held
       y.toString();  // OK: the lock is known to be held
       myMethod().toString(); // OK: the lock is known to be held
     }
  }

    Object a = new Object();
    Object b = new Object();
    @GuardedBy("a") Object x5 = new Object();
    @GuardedBy({"a", "b"}) Object y5 = new Object();
    void myMethod2() {
        //:: error: (assignment.type.incompatible)
        y5 = x5; // ILLEGAL
    }


    @GuardedBy({}) Object o1;
    @GuardedBy("lock") Object o2;
    @GuardedBy("lock") Object o3;

    void someMethod() {
      o3 = o2; // OK, since o2 and o3 are guarded by exactly the same lock set.

      //:: error: (assignment.type.incompatible)
      o1 = o2; // Assignment type incompatible errors are issued for both assignments, since
      //:: error: (assignment.type.incompatible)
      o2 = o1; // {"lock"} and {} are not identical sets.
    }

    @SuppressWarnings("lock:cast.unsafe")
    void someMethod2() {
       o1 = (@GuardedBy({}) Object) o2; // A cast can be used if the user knows it is safe to do so. However the @SuppressWarnings must be added.
    }

    static Object myLock = new Object();

 @GuardedBy("ChapterExamples.myLock") Object myMethod3() { return new Object(); }

  // reassignments without holding the lock are OK.
  @GuardedBy("ChapterExamples.myLock") Object x2 = myMethod3();
  @GuardedBy("ChapterExamples.myLock") Object y2 = x2;

  void myMethod4() {
    //:: error: (contracts.precondition.not.satisfied.field)
    x2.toString(); // ILLEGAL because the lock is not held
    synchronized(ChapterExamples.myLock) {
      y2.toString();  // OK: the lock is held
    }
  }

 void myMethod5(@GuardedBy("ChapterExamples.myLock") Object a) {
    //:: error: (contracts.precondition.not.satisfied.field)
    a.toString(); // ILLEGAL: the lock is not held
    synchronized(ChapterExamples.myLock) {
      a.toString();  // OK: the lock is held
    }
  }


@LockingFree
boolean compare(@GuardSatisfied Object a, @GuardSatisfied Object b){ return true; }

@GuardedBy({}) Object p1;
@GuardedBy("lock") Object p2;

void myMethod6(){
  synchronized(lock){ // It is the responsibility of callers to 'compare' to acquire the lock.
      boolean b1 = compare(p1, p2); // OK. No error issued.
  }
  //:: error: (contracts.precondition.not.satisfied.field)
  p2.toString();
  //:: error: (contracts.precondition.not.satisfied.field)
  boolean b2 = compare(p1, p2); // An error is issued indicating that p2 might be dereferenced without "lock" being held. The method call need not be modified, since @GuardedBy({}) <: @GuardedByInaccessible and @GuardedBy("lock") <: @GuardedByInaccessible, but the lock must be acquired prior to the method call.
}

 void helper1(@GuardedBy("ChapterExamples.myLock") Object a) {
    //:: error: (contracts.precondition.not.satisfied.field)
    a.toString(); // ILLEGAL: the lock is not held
    synchronized(ChapterExamples.myLock) {
      a.toString();  // OK: the lock is held
    }
  }
  @Holding("ChapterExamples.myLock")
  @LockingFree
  void helper2(@GuardedBy("ChapterExamples.myLock") Object b) {
    b.toString(); // OK: the lock is held
  }
  @LockingFree
  void helper3(@GuardSatisfied Object c) {
    c.toString(); // OK: the guard is satisfied
  }
  @LockingFree
  void helper4(@GuardedBy("ChapterExamples.myLock") Object d) {
    //:: error: (contracts.precondition.not.satisfied.field)
    d.toString(); // ILLEGAL: the lock is not held
  }
  void myMethod2(@GuardedBy("ChapterExamples.myLock") Object e) {
    helper1(e);  // OK to pass to another routine without holding the lock.
    //:: error: (contracts.precondition.not.satisfied.field)
    e.toString(); // ILLEGAL: the lock is not held
    synchronized (ChapterExamples.myLock) {
      helper2(e);
      helper3(e); // OK, since parameter is @GuardSatisfied
      helper4(e); // OK, but helper4's body still has an error.
    }
  }

private Object myField;
private ReentrantLock myLock2; // Initialized in the constructor
private @GuardedBy("myLock2") Object x3; // Initialized in the constructor

// This method does not use locks or synchronization but cannot
// be annotated as @SideEffectFree since it alters myField.
@LockingFree
void myMethod5() {
    myField = new Object();
}

@SideEffectFree
int mySideEffectFreeMethod() {
    return 0;
}

void myUnlockingMethod() {
    myLock2.unlock();
}

void myUnannotatedEmptyMethod() {
}

void myOtherMethod() {
    if (myLock2.tryLock()) {
        x3.toString(); // OK: the lock is held
        myMethod5();
        x3.toString(); // OK: the lock is still known to be held since myMethod is locking-free
        mySideEffectFreeMethod();
        x3.toString(); // OK: the lock is still known to be held since mySideEffectFreeMethod
                      // is side-effect-free
        myUnlockingMethod();
        //:: error: (contracts.precondition.not.satisfied.field)
        x3.toString(); // ILLEGAL: myLockingMethod is not locking-free
    }
    if (myLock2.tryLock()) {
        x3.toString(); // OK: the lock is held
        myUnannotatedEmptyMethod();
        //:: error: (contracts.precondition.not.satisfied.field)
        x3.toString(); // ILLEGAL: even though myUnannotatedEmptyMethod is empty, since it is
                      // not annotated with @LockingFree, the Lock Checker no longer knows
                      // the state of the lock.
        if (myLock2.isHeldByCurrentThread()) {
            x3.toString(); // OK: the lock is known to be held
        }
    }
}

void boxingUnboxing() {
    @GuardedBy("lock") int a = 1;
    @GuardedBy({}) Integer c;
    synchronized(lock) {
        c = a;
    }

    @GuardedBy("lock") Integer b = 1;
    @GuardedBy({}) int d;
    synchronized(lock) {
        //:: error: (assignment.type.incompatible)
        d = b; // TODO: This should not result in assignment.type.incompatible because 'b' is actually syntactic sugar for b.intValue(). See the explanation in LockVisitor.checkAccess for more information.
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
}

}
