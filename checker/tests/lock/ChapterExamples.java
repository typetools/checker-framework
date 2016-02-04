// This test contains the sample code from the Lock Checker manual chapter
// modified to fit testing instead of illustrative purposes,
// and contains other miscellaneous Lock Checker testing.
import org.checkerframework.checker.lock.qual.*;
import org.checkerframework.checker.nullness.qual.*;
import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.framework.qual.ImplicitFor;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedPrimitiveType;

import java.util.concurrent.locks.ReentrantLock;
import java.util.*;

class ChapterExamples {
    // This code crashed when there was a bug before issue 524 was fixed.
    // An attempt to take the LUB between 'val' in the store with type 'long'
    // and 'val' in another store with type 'none' resulted in a crash.
    private void foo (boolean b, int a) {
        if (b) {
          if (a == 0) {
            boolean val = false;
          } else if (a == 1) {
            int val = 0;
          } else if (a == 2) {
            long val = 0;
          } else if (a == 3) {
          }
        } else {
          if (true) {
          }
        }
    }


    private abstract class Values<V> extends AbstractCollection<V> {
        public <T> T[] toArray(T[] a) {
            Collection<V> c = new ArrayList<V>(size());
            for (Iterator<V> i = iterator(); i.hasNext(); )
                c.add(i.next());
            return c.toArray(a);
        }
    }



    // Keep these as test cases!
    <T> T method1(T t, boolean b) {
        //(return.type.incompatible)
        return b ? null : t;
//  error: [return.type.incompatible] incompatible types in return.
//        return b ? null : t;
//                 ^
//  found   : T[ extends @Initialized @Nullable Object super @Initialized @Nullable Void]
//  required: T[ extends @Initialized @Nullable Object super @Initialized @NonNull Void]
    }

    <T> T method2(T t, boolean b) {
        //(return.type.incompatible)
        return null;
//  error: [return.type.incompatible] incompatible types in return.
//        return null;
//               ^
//  found   : @FBCBottom @Nullable  null
//  required: T[ extends @Initialized @Nullable Object super @Initialized @NonNull Void]
    }

    //  The following code type checks (as expected):
    void bar(@NonNull Object nn1, boolean b) {
        @NonNull Object nn2 = method1(nn1, b);
        @NonNull Object nn3 = method2(nn1, b);
    }

    //  The following code type checks (as expected):
    void bar2(@LockHeld Object heldlock1, boolean b) {
        @LockPossiblyHeld Object heldlock2 = method1(heldlock1, b);
        @LockPossiblyHeld Object heldlock3 = method2(heldlock1, b);
    }

   //MyClass2<@NonNull Object> m;

   /*

    boolean b1;
    T ooo;
    T t1 = foo(ooo, b1);
    T t2 = foo(ooo, b1);
   }*/

    private static boolean eq(@GuardSatisfied Object o1, @GuardSatisfied Object o2) {
        return (o1 == null ? o2 == null : o1.equals(o2));
    }

    public <K extends @GuardedBy({}) Object,V extends @GuardedBy({}) Object> void put(K key, V value) {
        @SuppressWarnings("unchecked")
        K k = (K) maskNull(key);
    }

    class GuardedByInaccessibleTest<T extends @GuardedByInaccessible MyClass> {

        T m;

        void test() {
            //:: error: (method.invocation.invalid)
            m.method();

            @GuardedByInaccessible MyClass local = new @GuardedByInaccessible MyClass();
            //:: error: (cannot.dereference)
            local.field = new Object();
            //:: error: (method.invocation.invalid)
            local.method();

            //:: error: (cannot.dereference)
            m.field = new Object();
        }
    }
    class MyClass {
     Object field = new Object();
     @LockingFree
     Object method(@GuardSatisfied MyClass this){return new Object();}
     @LockingFree
     public @GuardSatisfied(1) MyClass append(@GuardSatisfied(1) MyClass this,
                                              @GuardSatisfied(2) MyClass m) {
         return this;
     }

     Object myLock;
     void testCallToMethod(@GuardedBy("myLock") MyClass this) {
         //:: error: (contracts.precondition.not.satisfied)
         this.method(); // method()'s receiver is annotated as @GuardSatisfied
     }
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

    //:: error: (primitive.type.guardedby)
    @GuardedBy("a") String s = "string";


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

// TODO: For now, boxed types are treated as primitive types. This may change in the future.
void unboxing() {
    int a = 1;
    //:: error: (primitive.type.guardedby)
    @GuardedBy("lock") Integer c;
    synchronized(lock) {
        //:: error: (assignment.type.incompatible)
        c = a;
    }

    //:: error: (primitive.type.guardedby)
    @GuardedBy("lock") Integer b = 1;
    int d;
    synchronized(lock) {
        //:: error: (assignment.type.incompatible)
        d = b;

        // Expected, since b cannot be @GuardedBy("lock") since it is a boxed primitive.
        //:: error: (method.invocation.invalid)
        d = b.intValue(); // The de-sugared version does not issue an error.
    }

    c = c + b; // Syntactic sugar for c = new Integer(c.intValue() + b.intValue()).

    // Expected, since b and c cannot be @GuardedBy("lock") since they are boxed primitives.
    //:: error: (method.invocation.invalid)
    c = new Integer(c.intValue() + b.intValue()); // The de-sugared version

    synchronized(lock) {
        c = c + b; // Syntactic sugar for c = new Integer(c.intValue() + b.intValue()).

        // Expected, since b and c cannot be @GuardedBy("lock") since they are boxed primitives.
        //:: error: (method.invocation.invalid)
        c = new Integer(c.intValue() + b.intValue()); // The de-sugared version
    }

    //:: error: (assignment.type.incompatible)
    a = b;
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
@GuardedBy("lock1") MyClass filename;
@GuardedBy("lock2") MyClass extension;

void method0() {
    //:: error: (contracts.precondition.not.satisfied) :: error: (contracts.precondition.not.satisfied.field)
    filename = filename.append(extension);
}

void method1() {
    lock1.lock();
    //:: error: (contracts.precondition.not.satisfied.field)
    filename = filename.append(extension);
}

void method2() {
    lock2.lock();
    //:: error: (contracts.precondition.not.satisfied)
    filename = filename.append(extension);
}

void method3() {
    lock1.lock();
    lock2.lock();
    filename = filename.append(extension);
    filename = filename.append(null);
    //:: error: (assignment.type.incompatible)
    filename = extension.append(extension);
    //:: error: (assignment.type.incompatible)
    filename = extension.append(filename);
}

void matchingGSparams(@GuardSatisfied(1) MyClass m1,
                      @GuardSatisfied(1) MyClass m2) {
}

void method4() {
    lock1.lock();
    lock2.lock();
    matchingGSparams(filename, null);
    matchingGSparams(null, filename);
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

   public static boolean deepEquals(Object o1, Object o2) {
    if (o1 instanceof Object[] && o2 instanceof Object[]) {
      return Arrays.deepEquals((Object[]) o1, (Object[]) o2);
    }
    return false;
   }

  public static final class Comparer<T extends Comparable<T>> {
    public boolean compare(T[] a1, T[] a2) {
        T elt1 = a1[0];
        T elt2 = a2[0];
        return elt1.equals(elt2);
  }
  }

  public static <T extends @GuardedBy({}) Object> boolean indexOf(T[] a, Object elt) {
      if (elt.equals(a[0]))
          return false;
      return true;
//      found   : (@org.checkerframework.checker.lock.qual.GuardedBy({}) :: T)[ extends @GuardedByInaccessible @LockPossiblyHeld Object super @GuardedBy({}) @LockHeld Void]
//              required: @GuardedBy @LockPossiblyHeld Object
    }

  private static final Object NULL_KEY = new Object();
  @SuppressWarnings("cast.unsafe")
  private static @GuardSatisfied(1) Object maskNull(@GuardSatisfied(1) Object key) {
      return (key == null ? (@GuardSatisfied(1) Object) NULL_KEY : key);
  }

  // Tests that @GuardedBy({}) is @ImplicitFor(typeNames = { java.lang.String.class })
  void StringIsGBnothing(@GuardedByInaccessible Object o) {
      //:: error: (assignment.type.incompatible)
      String s = (String) o;
  }

  // Tests that the resulting type of string concatenation is always @GuardedBy({})
  // (and not @GuardedByInaccessible, which is the LUB of @GuardedBy({}) (the type of the
  // string literal "a") and @GuardSatisfied (the type of param))
  void StringConcat(/*@GuardSatisfied*/ MyClass param) {
      String s = "a" + param;
  }

  public void assignmentOfGSWithNoIndex(@GuardSatisfied Object a, @GuardSatisfied Object b) {
      //:: error: (guardsatisfied.assignment.disallowed)
      a = b;
  }


  class TestConcurrentSemantics1 {
	  /* This class tests the following critical scenario.
	   * 
	   * Suppose the following lines from method1 are executed on thread A.
	   * 
	   * @GuardedBy(“lock1”) MyClass local;
	   * m = local;
	   * 
	   * Then a context switch occurs to method2 on thread B and the following lines are executed:
	   * 
	   * @GuardedBy(“lock2”) MyClass local;
	   * m = local;
	   * 
	   * Then a context switch back to method1 on thread A occurs and the following lines are executed:
	   * 
	   * lock1.lock();
	   * m.field = new Object();
	   * 
	   * In this case, it is absolutely critical that the dereference above not be allowed.
	   * 
	   */
	  	  
        @GuardedByInaccessible MyClass m;
        ReentrantLock lock1 = new ReentrantLock();
        ReentrantLock lock2 = new ReentrantLock();

        void method1() {
            @GuardedBy("lock1") MyClass local = new MyClass();
            m = local;
            lock1.lock();
            //:: error: (cannot.dereference)
            m.field = new Object();
        }

        void method2() {
            @GuardedBy("lock2") MyClass local = new MyClass();
            m = local;
        }
    }
  
  class TestConcurrentSemantics2 {
      @GuardedBy("a") Object o;

      void method() {
          o = null;
          // Assume the following happens:
		  // Context switch to a different thread
		  // bar() is called on the other thread
		  // Context switch back to this thread
          //:: error: (assignment.type.incompatible)
		  @GuardedBy("b") Object o2 = o; // o is no longer null and an assignment.type.incompatible error should be issued
      }

      void bar(){
          o = new Object();
      }
      
      // Test that field assignments do not cause their type to be refined:
	  @GuardedBy("a") Object myObject1 = null;
	  //:: error: (assignment.type.incompatible)
	  @GuardedBy("b") Object myObject2 = myObject1;
  }

}
