// This test contains the sample code from the Lock Checker manual chapter
// modified to fit testing instead of illustrative purposes,
// and contains other miscellaneous Lock Checker testing.

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.locks.ReentrantLock;
import org.checkerframework.checker.lock.qual.GuardSatisfied;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.checkerframework.checker.lock.qual.GuardedByBottom;
import org.checkerframework.checker.lock.qual.GuardedByUnknown;
import org.checkerframework.checker.lock.qual.Holding;
import org.checkerframework.checker.lock.qual.LockingFree;
import org.checkerframework.checker.lock.qual.MayReleaseLocks;
import org.checkerframework.checker.lock.qual.ReleasesNoLocks;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.dataflow.qual.Deterministic;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;

class ChapterExamples {
    // This code crashed when there was a bug before issue 524 was fixed.
    // An attempt to take the LUB between 'val' in the store with type 'long'
    // and 'val' in another store with type 'none' resulted in a crash.
    private void foo(boolean b, int a) {
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
            if (true) {}
        }
    }

    private abstract class Values<V> extends AbstractCollection<V> {
        public <T> T[] toArray(T[] a) {
            Collection<V> c = new ArrayList<V>(size());
            for (Iterator<V> i = iterator(); i.hasNext(); ) {
                c.add(i.next());
            }
            return c.toArray(a);
        }
    }

    // @GuardedByBottom, which represents the 'null' literal, is the default lower bound,
    // so null can be returned in the following two methods:
    <T> T method1(T t, boolean b) {
        return b ? null : t;
    }

    <T> T method2(T t, boolean b) {
        return null;
    }

    void bar(@NonNull Object nn1, boolean b) {
        @NonNull Object nn2 = method1(nn1, b);
        @NonNull Object nn3 = method2(nn1, b);
    }

    void bar2(@GuardedByBottom Object bottomParam, boolean b) {
        @GuardedByUnknown Object refinedToBottom1 = method1(bottomParam, b);
        @GuardedByUnknown Object refinedToBottom2 = method2(bottomParam, b);
        @GuardedByBottom Object bottom1 = method1(bottomParam, b);
        @GuardedByBottom Object bottom2 = method2(bottomParam, b);
    }

    private static boolean eq(@GuardSatisfied Object o1, @GuardSatisfied Object o2) {
        return (o1 == null ? o2 == null : o1.equals(o2));
    }

    public <K extends @GuardedBy({}) Object, V extends @GuardedBy({}) Object> void put(
            K key, V value) {
        @SuppressWarnings("unchecked")
        K k = (K) maskNull(key);
    }

    class GuardedByUnknownTest<T extends @GuardedByUnknown MyClass> {

        T m;

        void test() {
            //:: error: (method.invocation.invalid)
            m.method();

            @GuardedByUnknown MyClass local = new @GuardedByUnknown MyClass();
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
        Object method(@GuardSatisfied MyClass this) {
            return new Object();
        }

        @LockingFree
        public @GuardSatisfied(1) MyClass append(
                @GuardSatisfied(1) MyClass this, @GuardSatisfied(2) MyClass m) {
            return this;
        }

        final Object myLock = new Object();

        void testCallToMethod(@GuardedBy("myLock") MyClass this) {
            //:: error: (contracts.precondition.not.satisfied)
            this.method(); // method()'s receiver is annotated as @GuardSatisfied
        }
    }

    @MayReleaseLocks
    @ReleasesNoLocks
    // TODO: enable (multiple.sideeffect.annotation)
    void testMultipleSideEffectAnnotations() {}

    void guardedByItselfOnReceiver(@GuardedBy("<self>") ChapterExamples this) {
        synchronized (
                this) { // Tests translation of '<self>' to 'this' by the LockVisitor for this scenario.
            // myField = new MyClass();
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
        synchronized (this) {
            myField = new MyClass();
            myField.toString();
            this.myField = new MyClass();
            this.myField.toString();
        }
    }

    void testDereferenceOfReceiverAndParameter(
            @GuardedBy("lock") ChapterExamples this, @GuardedBy("lock") MyClass m) {
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
        // The following error is due to the fact that you cannot access "this.lock" without first having acquired "lock".
        // The right fix in a user scenario would be to not guard "this" with "this.lock". The current object could instead
        // be guarded by "<self>" or by some other lock expression that is not one of its fields. We are keeping this test
        // case here to make sure this scenario issues a warning.
        //:: error: (contracts.precondition.not.satisfied.field)
        synchronized (lock) {
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
    @GuardedBy("lock") MyClass myMethodReturningMyObj() {
        return myObj;
    }

    ChapterExamples() {
        lock = new Object();
    }

    void myMethod8() {
        //:: error: (contracts.precondition.not.satisfied)
        boolean b4 = compare(p1, myMethod());

        // An error is issued indicating that p2 might be dereferenced without
        // "lock" being held. The method call need not be modified, since
        // @GuardedBy({}) <: @GuardedByUnknown and @GuardedBy("lock") <: @GuardedByUnknown,
        // but the lock must be acquired prior to the method call.
        //:: error: (contracts.precondition.not.satisfied.field)
        boolean b2 = compare(p1, p2);
        //:: error: (contracts.precondition.not.satisfied.field)
        boolean b3 = compare(p1, this.p2);
        //:: error: (contracts.precondition.not.satisfied)
        boolean b5 = compare(p1, this.myMethod());
        synchronized (lock) {
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

    void myMethod7() {
        //:: error: (contracts.precondition.not.satisfied.field)
        Object f = myObj.field;
        //:: error: (contracts.precondition.not.satisfied)
        Object f2 = myMethodReturningMyObj().field;
        //:: error: (contracts.precondition.not.satisfied)
        myObj.method(); // method()'s receiver is annotated as @GuardSatisfied
        //:: error: (contracts.precondition.not.satisfied)
        myMethodReturningMyObj().method(); // method()'s receiver is annotated as @GuardSatisfied

        synchronized (lock) {
            f = myObj.field;
            f2 = myMethodReturningMyObj().field;
            myObj.method();
            myMethodReturningMyObj().method();
        }

        //:: error: (contracts.precondition.not.satisfied)
        myMethodReturningMyObj().field = new Object();
        //:: error: (contracts.precondition.not.satisfied.field)
        x.field = new Object();
        synchronized (lock) {
            myMethod().field = new Object();
        }
        synchronized (lock) {
            x.field = new Object(); // toString is not LockingFree. How annoying.
        }

        this.x = new MyClass();
    }

    final Object lock; // Initialized in the constructor

    @GuardedBy("lock") MyClass x = new MyClass();

    @GuardedBy("lock") MyClass y = x; // OK, because dereferences of y will require "lock" to be held.
    //:: error: (assignment.type.incompatible)
    @GuardedBy({}) MyClass z = x; // ILLEGAL because dereferences of z do not require "lock" to be held.

    @LockingFree
    @GuardedBy("lock") MyClass myMethod() {
        return x; // OK because the return type is @GuardedBy("lock")
    }

    void exampleMethod() {
        //:: error: (contracts.precondition.not.satisfied.field)
        x.field = new Object(); // ILLEGAL because the lock is not known to be held
        //:: error: (contracts.precondition.not.satisfied.field)
        y.field = new Object(); // ILLEGAL because the lock is not known to be held
        //:: error: (contracts.precondition.not.satisfied)
        myMethod().field = new Object(); // ILLEGAL because the lock is not known to be held
        synchronized (lock) {
            x.field = new Object(); // OK: the lock is known to be held
            y.field = new Object(); // OK: the lock is known to be held
            myMethod().field = new Object(); // OK: the lock is known to be held
        }
    }

    final MyClass a = new MyClass();
    final MyClass b = new MyClass();

    @GuardedBy("a") MyClass x5 = new MyClass();

    @GuardedBy({"a", "b"}) MyClass y5 = new MyClass();

    void myMethod2() {
        //:: error: (assignment.type.incompatible)
        y5 = x5; // ILLEGAL
    }

    //:: error: (immutable.type.guardedby)
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
        // A cast can be used if the user knows it is safe to do so.
        // However, the @SuppressWarnings must be added.
        o1 = (@GuardedBy({}) MyClass) o2;
    }

    final static Object myLock = new Object();

    @GuardedBy("ChapterExamples.myLock") MyClass myMethod3() {
        return new MyClass();
    }

    // reassignments without holding the lock are OK.
    @GuardedBy("ChapterExamples.myLock") MyClass x2 = myMethod3();

    @GuardedBy("ChapterExamples.myLock") MyClass y2 = x2;

    void myMethod4() {
        //:: error: (contracts.precondition.not.satisfied.field)
        x2.field = new Object(); // ILLEGAL because the lock is not held
        synchronized (ChapterExamples.myLock) {
            y2.field = new Object(); // OK: the lock is held
        }
    }

    void myMethod5(@GuardedBy("ChapterExamples.myLock") MyClass a) {
        //:: error: (contracts.precondition.not.satisfied.field)
        a.field = new Object(); // ILLEGAL: the lock is not held
        synchronized (ChapterExamples.myLock) {
            a.field = new Object(); // OK: the lock is held
        }
    }

    @LockingFree
    boolean compare(@GuardSatisfied MyClass a, @GuardSatisfied MyClass b) {
        return true;
    }

    @GuardedBy({}) MyClass p1;

    @GuardedBy("lock") MyClass p2;

    void myMethod6() {
        // It is the responsibility of callers to 'compare' to acquire the lock.
        synchronized (lock) {
            boolean b1 = compare(p1, p2); // OK. No error issued.
        }
        //:: error: (contracts.precondition.not.satisfied.field)
        p2.field = new Object();
        // An error is issued indicating that p2 might be dereferenced without "lock" being held. The method call need not be modified, since @GuardedBy({}) <: @GuardedByUnknown and @GuardedBy("lock") <: @GuardedByUnknown, but the lock must be acquired prior to the method call.
        //:: error: (contracts.precondition.not.satisfied.field)
        boolean b2 = compare(p1, p2);
    }

    void helper1(@GuardedBy("ChapterExamples.myLock") MyClass a) {
        //:: error: (contracts.precondition.not.satisfied.field)
        a.field = new Object(); // ILLEGAL: the lock is not held
        synchronized (ChapterExamples.myLock) {
            a.field = new Object(); // OK: the lock is held
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
    void helper5() {}
    // No annotation means @ReleasesNoLocks
    void helper6() {}

    void myMethod2(@GuardedBy("ChapterExamples.myLock") MyClass e) {
        helper1(e); // OK to pass to another routine without holding the lock.
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
    private final ReentrantLock myLock2 = new ReentrantLock();
    private @GuardedBy("myLock2") MyClass x3;

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
    void myReleaseLocksEmptyMethod() {}

    @MayReleaseLocks
    //:: error: (guardsatisfied.with.mayreleaselocks)
    void methodGuardSatisfiedReceiver(@GuardSatisfied ChapterExamples this) {}

    @MayReleaseLocks
    //:: error: (guardsatisfied.with.mayreleaselocks)
    void methodGuardSatisfiedParameter(@GuardSatisfied Object o) {}

    @MayReleaseLocks
    void myOtherMethod() {
        if (myLock2.tryLock()) {
            x3.field = new Object(); // OK: the lock is held
            myMethod5();
            x3.field = new Object(); // OK: the lock is still held since myMethod is locking-free
            mySideEffectFreeMethod();
            x3.field =
                    new Object(); // OK: the lock is still held since mySideEffectFreeMethod is side-effect-free
            myUnlockingMethod();
            //:: error: (contracts.precondition.not.satisfied.field)
            x3.field = new Object(); // ILLEGAL: myLockingMethod is not locking-free
        }
        if (myLock2.tryLock()) {
            x3.field = new Object(); // OK: the lock is held
            myReleaseLocksEmptyMethod();
            //:: error: (contracts.precondition.not.satisfied.field)
            x3.field =
                    new Object(); // ILLEGAL: even though myUnannotatedEmptyMethod is empty, since
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
        //:: error: (immutable.type.guardedby)
        @GuardedBy("lock") Integer c;
        synchronized (lock) {
            //:: error: (assignment.type.incompatible)
            c = a;
        }

        //:: error: (immutable.type.guardedby)
        @GuardedBy("lock") Integer b = 1;
        int d;
        synchronized (lock) {
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

        synchronized (lock) {
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
        d = b; // TODO: This should not result in assignment.type.incompatible because 'b' is actually syntactic sugar for b.intValue().
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
      a = b; // TODO: This assignment between two reference types should not require a lock to be held.
    }*/

    final ReentrantLock lock1 = new ReentrantLock();
    final ReentrantLock lock2 = new ReentrantLock();

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

    void matchingGSparams(@GuardSatisfied(1) MyClass m1, @GuardSatisfied(1) MyClass m2) {}

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
            void innerClassMethod() {}
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
        if (elt.equals(a[0])) {
            return false;
        }
        return true;
        //    found   : (@org.checkerframework.checker.lock.qual.GuardedBy({}) :: T)[ extends @GuardedByUnknown @LockPossiblyHeld Object super @GuardedBy({}) @LockHeld Void]
        //        required: @GuardedBy @LockPossiblyHeld Object
    }

    private static final Object NULL_KEY = new Object();
    // A guardsatisfied.location.disallowed error is issued for the cast.
    @SuppressWarnings({"cast.unsafe", "guardsatisfied.location.disallowed"})
    private static @GuardSatisfied(1) Object maskNull(@GuardSatisfied(1) Object key) {
        return (key == null ? (@GuardSatisfied(1) Object) NULL_KEY : key);
    }

    // Tests that @GuardedBy({}) is @ImplicitFor(typeNames = { java.lang.String.class })
    void StringIsGBnothing(
            @GuardedByUnknown Object o1,
            @GuardedBy("lock") Object o2,
            @GuardSatisfied Object o3,
            @GuardedByBottom Object o4) {
        //:: error: (assignment.type.incompatible)
        String s1 = (String) o1;
        //:: error: (assignment.type.incompatible)
        String s2 = (String) o2;
        //:: error: (assignment.type.incompatible)
        String s3 = (String) o3;
        String s4 = (String) o4; // OK
    }

    // Tests that the resulting type of string concatenation is always @GuardedBy({})
    // (and not @GuardedByUnknown, which is the LUB of @GuardedBy({}) (the type of the
    // string literal "a") and @GuardedBy("lock") (the type of param))
    void StringConcat(@GuardedBy("lock") MyClass param) {
        {
            String s1a = "a" + "a";
            //:: error: (contracts.precondition.not.satisfied.field)
            String s1b = "a" + param;
            //:: error: (contracts.precondition.not.satisfied.field)
            String s1c = param + "a";
            //:: error: (contracts.precondition.not.satisfied)
            String s1d = param.toString();

            String s2 = "a";
            //:: error: (contracts.precondition.not.satisfied.field)
            s2 += param;

            String s3 = "a";
            // In addition to testing whether "lock" is held, tests that the result of a string concatenation has type @GuardedBy({}).
            //:: error: (contracts.precondition.not.satisfied.field)
            String s4 = s3 += param;
        }
        synchronized (lock) {
            String s1a = "a" + "a";
            String s1b = "a" + param;
            String s1c = param + "a";
            String s1d = param.toString();

            String s2 = "a";
            s2 += param;

            String s3 = "a";
            // In addition to testing whether "lock" is held, tests that the result of a string concatenation has type @GuardedBy({}).
            String s4 = s3 += param;
        }
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
         * @GuardedBy("lock1") MyClass local;
         * m = local;
         *
         * Then a context switch occurs to method2 on thread B and the following lines are executed:
         *
         * @GuardedBy("lock2") MyClass local;
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

        @GuardedByUnknown MyClass m;
        final ReentrantLock lock1 = new ReentrantLock();
        final ReentrantLock lock2 = new ReentrantLock();

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
            //  * Context switch to a different thread.
            //  * bar() is called on the other thread.
            //  * Context switch back to this thread.
            // o is no longer null and an assignment.type.incompatible error should be issued.
            //:: error: (assignment.type.incompatible)
            @GuardedBy("b") Object o2 = o;
        }

        void bar() {
            o = new Object();
        }

        // Test that field assignments do not cause their type to be refined:
        @GuardedBy("a") Object myObject1 = null;
        //:: error: (assignment.type.incompatible)
        @GuardedBy("b") Object myObject2 = myObject1;
    }

    @MayReleaseLocks
    synchronized void mayReleaseLocksSynchronizedMethod() {}

    @ReleasesNoLocks
    synchronized void releasesNoLocksSynchronizedMethod() {}

    @LockingFree
    //:: error: (lockingfree.synchronized.method)
    synchronized void lockingFreeSynchronizedMethod() {}

    @SideEffectFree
    //:: error: (lockingfree.synchronized.method)
    synchronized void sideEffectFreeSynchronizedMethod() {}

    @Pure
    //:: error: (lockingfree.synchronized.method)
    synchronized void pureSynchronizedMethod() {}

    @MayReleaseLocks
    void mayReleaseLocksMethodWithSynchronizedBlock() {
        synchronized (this) {}
    }

    @ReleasesNoLocks
    void releasesNoLocksMethodWithSynchronizedBlock() {
        synchronized (this) {}
    }

    @LockingFree
    void lockingFreeMethodWithSynchronizedBlock() {
        //:: error: (synchronized.block.in.lockingfree.method)
        synchronized (this) {}
    }

    @SideEffectFree
    void sideEffectFreeMethodWithSynchronizedBlock() {
        //:: error: (synchronized.block.in.lockingfree.method)
        synchronized (this) {}
    }

    @Pure
    void pureMethodWithSynchronizedBlock() {
        //:: error: (synchronized.block.in.lockingfree.method)
        synchronized (this) {}
    }

    //:: error: (class.declaration.guardedby.annotation.invalid)
    @GuardedByUnknown class MyClass2 {}
    //:: error: (class.declaration.guardedby.annotation.invalid) :: error: (lock.expression.possibly.not.final)
    @GuardedBy("lock") class MyClass3 {}

    @GuardedBy({}) class MyClass4 {}
    //:: error: (class.declaration.guardedby.annotation.invalid) :: error: (guardsatisfied.location.disallowed)
    @GuardSatisfied class MyClass5 {}
    //:: error: (class.declaration.guardedby.annotation.invalid)
    @GuardedByBottom class MyClass6 {}

    class C1 {
        final C1 field =
                new C1(); // Infinite loop. This code is not meant to be executed, only type checked.
        C1 field2;

        @Deterministic
        C1 getFieldDeterministic() {
            return field;
        }

        @Pure
        C1 getFieldPure(Object param1, Object param2) {
            return field;
        }

        @Pure
        C1 getFieldPure2() {
            return field;
        }

        C1 getField() {
            return field;
        }
    }

    final C1 c1 = new C1();

    // Analogous to testExplicitLockExpressionIsFinal and testGuardedByExpressionIsFinal, but for monitor locks acquired in synchronized blocks.
    void testSynchronizedExpressionIsFinal(boolean b) {
        synchronized (c1) {}

        Object o1 = new Object(); // o1 is effectively final - it is never reassigned
        Object o2 = new Object(); // o2 is reassigned later - it is not effectively final
        synchronized (o1) {}
        //:: error: (lock.expression.not.final)
        synchronized (o2) {}

        o2 = new Object(); // Reassignment that makes o2 not have been effectively final earlier.

        // Tests that package names are considered final.
        synchronized (java.lang.String.class) {}

        // Test a tree that is not supported by LockVisitor.ensureExpressionIsEffectivelyFinal
        //:: error: (lock.expression.possibly.not.final)
        synchronized (c1.getFieldPure(b ? c1 : o1, c1)) {}

        synchronized (
                c1.field.field.field.getFieldPure(
                                c1.field, c1.getFieldDeterministic().getFieldPure(c1, c1.field))
                        .field) {}

        // The following negative test cases are the same as the one above but with one modification in each.

        synchronized (
                //:: error: (lock.expression.not.final)
                c1.field.field2.field.getFieldPure(
                                c1.field, c1.getFieldDeterministic().getFieldPure(c1, c1.field))
                        .field) {}
        synchronized (
                c1.field.field.field.getFieldPure(
                                //:: error: (lock.expression.not.final)
                                c1.field, c1.getField().getFieldPure(c1, c1.field))
                        .field) {}
    }

    class C2 extends ReentrantLock {
        final C2 field =
                new C2(); // Infinite loop. This code is not meant to be executed, only type checked.
        C2 field2;

        @Deterministic
        C2 getFieldDeterministic() {
            return field;
        }

        @Pure
        C2 getFieldPure(Object param1, Object param2) {
            return field;
        }

        C2 getField() {
            return field;
        }
    }

    final C2 c2 = new C2();

    // Analogous to testSynchronizedExpressionIsFinal and testGuardedByExpressionIsFinal, but for explicit locks.
    @MayReleaseLocks
    void testExplicitLockExpressionIsFinal(boolean b) {
        c2.lock();

        ReentrantLock rl1 =
                new ReentrantLock(); // rl1 is effectively final - it is never reassigned
        ReentrantLock rl2 =
                new ReentrantLock(); // rl2 is reassigned later - it is not effectively final
        rl1.lock();
        rl1.unlock();
        //:: error: (lock.expression.not.final)
        rl2.lock();
        //:: error: (lock.expression.not.final)
        rl2.unlock();

        rl2 =
                new ReentrantLock(); // Reassignment that makes rl2 not have been effectively final earlier.

        // Test a tree that is not supported by LockVisitor.ensureExpressionIsEffectivelyFinal
        //:: error: (lock.expression.possibly.not.final)
        c2.getFieldPure(b ? c2 : rl1, c2).lock();
        //:: error: (lock.expression.possibly.not.final)
        c2.getFieldPure(b ? c2 : rl1, c2).unlock();

        c2.field
                .field
                .field
                .getFieldPure(c2.field, c2.getFieldDeterministic().getFieldPure(c2, c2.field))
                .field
                .lock();
        c2.field
                .field
                .field
                .getFieldPure(c2.field, c2.getFieldDeterministic().getFieldPure(c2, c2.field))
                .field
                .unlock();

        // The following negative test cases are the same as the one above but with one modification in each.

        c2.field
                //:: error: (lock.expression.not.final)
                .field2
                .field
                .getFieldPure(c2.field, c2.getFieldDeterministic().getFieldPure(c2, c2.field))
                .field
                .lock();
        c2.field
                //:: error: (lock.expression.not.final)
                .field2
                .field
                .getFieldPure(c2.field, c2.getFieldDeterministic().getFieldPure(c2, c2.field))
                .field
                .unlock();

        c2.field
                .field
                .field
                //:: error: (lock.expression.not.final)
                .getFieldPure(c2.field, c2.getField().getFieldPure(c2, c2.field))
                .field
                .lock();
        c2.field
                .field
                .field
                //:: error: (lock.expression.not.final)
                .getFieldPure(c2.field, c2.getField().getFieldPure(c2, c2.field))
                .field
                .unlock();
    }

    // Analogous to testSynchronizedExpressionIsFinal and testExplicitLockExpressionIsFinal, but for expressions in @GuardedBy annotations.
    void testGuardedByExpressionIsFinal() {
        @GuardedBy("c1") Object guarded1;

        final Object o1 = new Object();
        Object o2 = new Object();
        @GuardedBy("o1") Object guarded2 = new Object();
        //:: error: (lock.expression.not.final)
        @GuardedBy("o2") Object guarded3 = new Object();

        // Test expressions that are not supported by LockVisitor.ensureExpressionIsEffectivelyFinal
        @GuardedBy("java.lang.String.class") Object guarded4;
        //:: error: (flowexpr.parse.error)
        @GuardedBy("c1.getFieldPure(b ? c1 : o1, c1)")
        Object guarded5;

        // TODO: Fix the flow expression parser so it can handle
        // @GuardedBy("c1.field.field.field.getFieldPure(c1.field, c1.getFieldDeterministic().getFieldPure(c1, c1.field)).field") Object guarded6;
        // Currently it fails because the memberselect incorrectly splits the "getFieldPure(...).field" field access into:
        // "getFieldPure(c1"
        // and
        // "field, c1.getFieldDeterministic().getFieldPure(c1, c1.field)).field"
        // However, as soon as one fixes the flow expression parser to parse a longer expression, one must consider
        // whether the CFAbstractStore can (or should) store data for the resulting flow expression.

        @GuardedBy("c1.field.field.field.getFieldPure2().getFieldDeterministic().field")
        Object guarded6;

        // The following negative test cases are the same as the one above but with one modification in each.

        //:: error: (lock.expression.not.final)
        @GuardedBy("c1.field.field2.field.getFieldPure2().getFieldDeterministic().field")
        Object guarded7;
        //:: error: (lock.expression.not.final)
        @GuardedBy("c1.field.field.field.getField().getFieldDeterministic().field")
        Object guarded8;

        // Additional test cases to test that method parameters (in this case the parameters to getFieldPure) are parsed.
        @GuardedBy("c1.field.field.field.getFieldPure(c1, c1).getFieldDeterministic().field")
        Object guarded9;
        @GuardedBy("c1.field.field.field.getFieldPure(c1, o1).getFieldDeterministic().field")
        Object guarded10;
        //:: error: (lock.expression.not.final)
        @GuardedBy("c1.field.field.field.getFieldPure(c1, o2).getFieldDeterministic().field")
        Object guarded11;

        // Test that @GuardedBy annotations on various tree kinds inside a method are visited

        Object guarded12 = (@GuardedBy("o1") Object) guarded2;
        //:: error: (lock.expression.not.final)
        Object guarded13 = (@GuardedBy("o2") Object) guarded3;

        Object guarded14[] = new @GuardedBy("o1") MyClass[3];
        //:: error: (lock.expression.not.final)
        Object guarded15[] = new @GuardedBy("o2") MyClass[3];

        // Tests that the location of the @GB annotation inside a VariableTree does not matter (i.e. it does not need to be the leftmost subtree).
        Object guarded16 @GuardedBy("o1") [];
        //:: error: (lock.expression.not.final)
        Object guarded17 @GuardedBy("o2") [];

        @GuardedBy("o1") Object guarded18[];
        //:: error: (lock.expression.not.final)
        @GuardedBy("o2") Object guarded19[];

        // TODO: BaseTypeVisitor.visitAnnotation does not currently visit annotations on type arguments.
        // Address this for the Lock Checker somehow and enable the warnings below:
        MyParameterizedClass1<@GuardedBy("o1") Object> m1;
        // TODO: Enable :: error: (lock.expression.not.final)
        MyParameterizedClass1<@GuardedBy("o2") Object> m2;

        boolean b = c1 instanceof @GuardedBy("o1") Object;
        //:: error: (lock.expression.not.final)
        b = c1 instanceof @GuardedBy("o2") Object;

        // Additional tests just outside of this method below:
    }

    // Test that @GuardedBy annotations on various tree kinds outside a method are visited

    // Test that @GuardedBy annotations on method return types are visited. No need to test method receivers and parameters
    // as they are covered by tests above that visit VariableTree.

    final Object finalField = new Object();
    Object nonFinalField = new Object();

    @GuardedBy("finalField") Object testGuardedByExprIsFinal1() {
        return null;
    }

    //:: error: (lock.expression.not.final)
    @GuardedBy("nonFinalField") Object testGuardedByExprIsFinal2() {
        return null;
    }

    <T extends @GuardedBy("finalField") Object> T myMethodThatReturnsT_1(T t) {
        return t;
    }

    //:: error: (lock.expression.not.final)
    <T extends @GuardedBy("nonFinalField") Object> T myMethodThatReturnsT_2(T t) {
        return t;
    }

    class MyParameterizedClass1<T extends @GuardedByUnknown Object> {};

    // TODO: BaseTypeVisitor.visitAnnotation does not currently visit annotations on wildcard bounds.
    // Address this for the Lock Checker somehow and enable the warnings below:

    MyParameterizedClass1<? super @GuardedBy("finalField") Object> m1;
    // TODO: Enable :: error: (lock.expression.not.final)
    MyParameterizedClass1<? super @GuardedBy("nonFinalField") Object> m2;

    MyParameterizedClass1<? extends @GuardedBy("finalField") Object> m3;
    // TODO: Enable :: error: (lock.expression.not.final)
    MyParameterizedClass1<? extends @GuardedBy("nonFinalField") Object> m4;

    class MyClassContainingALock {
        final ReentrantLock finalLock = new ReentrantLock();
        ReentrantLock nonFinalLock = new ReentrantLock();
        Object field;
    }

    void testItselfFinalLock() {
        final @GuardedBy("<self>.finalLock") MyClassContainingALock m =
                new MyClassContainingALock();
        //:: error: (contracts.precondition.not.satisfied.field)
        m.field = new Object();
        // Ignore this error: it is expected that an error will be issued for dereferencing 'm' in order to take the 'm.finalLock' lock.
        // Typically, the Lock Checker does not support an object being guarded by one of its fields, but this is sometimes done in user code
        // with a ReentrantLock field guarding its containing object. This unfortunately makes it a bit difficult for users since they have
        // to add a @SuppressWarnings for this call while still making sure that warnings for other dereferences are not suppressed.
        //:: error: (contracts.precondition.not.satisfied.field)
        m.finalLock.lock();
        m.field = new Object();
    }

    void testItselfNonFinalLock() {
        final @GuardedBy("<self>.nonFinalLock") MyClassContainingALock m =
                new MyClassContainingALock();
        //:: error: (lock.expression.not.final) :: error: (contracts.precondition.not.satisfied.field)
        m.field = new Object();
        //:: error: (lock.expression.not.final) :: error: (contracts.precondition.not.satisfied.field)
        m.nonFinalLock.lock();
        //:: error: (lock.expression.not.final)
        m.field = new Object();
    }

    class Session {
        @Holding("this")
        public void kill(@GuardSatisfied Session this) {}
    }

    class SessionManager {
        private @GuardedBy("<self>") Session session = new Session();

        private void session_done() {
            final @GuardedBy("<self>") Session tmp = session;
            session = null;
            synchronized (tmp) {
                tmp.kill();
            }
        }
    }
}
