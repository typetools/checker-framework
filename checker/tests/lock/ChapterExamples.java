// This test contains the sample code from the Lock Checker manual chapter modified to fit testing
// instead of illustrative purposes, and contains other miscellaneous Lock Checker testing.

import org.checkerframework.checker.lock.qual.GuardSatisfied;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.checkerframework.checker.lock.qual.GuardedByBottom;
import org.checkerframework.checker.lock.qual.GuardedByUnknown;
import org.checkerframework.checker.lock.qual.Holding;
import org.checkerframework.checker.lock.qual.LockingFree;
import org.checkerframework.checker.lock.qual.MayReleaseLocks;
import org.checkerframework.checker.lock.qual.ReleasesNoLocks;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.locks.ReentrantLock;

public class ChapterExamples {
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
        @SuppressWarnings("method.guarantee.violated") // side effect is only to local iterator
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
            // :: error: (method.invocation.invalid)
            m.method();

            @GuardedByUnknown MyClass local = new @GuardedByUnknown MyClass();
            // :: error: (lock.not.held)
            local.field = new Object();
            // :: error: (method.invocation.invalid)
            local.method();

            // :: error: (lock.not.held)
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
            // :: error: (lock.not.held)
            this.method(); // method()'s receiver is annotated as @GuardSatisfied
        }
    }

    @MayReleaseLocks
    @ReleasesNoLocks
    // TODO: enable (multiple.sideeffect.annotation)
    void testMultipleSideEffectAnnotations() {}

    void guardedByItselfOnReceiver(@GuardedBy("<self>") ChapterExamples this) {
        synchronized (this) { // Tests translation of '<self>' to 'this'
            // myField = new MyClass();
            myField.toString();
            this.myField = new MyClass();
            this.myField.toString();
        }
        // :: error: (lock.not.held)
        myField = new MyClass();
        // :: error: (lock.not.held)
        myField.toString();
        // :: error: (lock.not.held)
        this.myField = new MyClass();
        // :: error: (lock.not.held)
        this.myField.toString();
    }

    void guardedByThisOnReceiver(@GuardedBy("this") ChapterExamples this) {
        // :: error: (lock.not.held)
        myField = new MyClass();
        // :: error: (lock.not.held)
        myField.toString();
        // :: error: (lock.not.held)
        this.myField = new MyClass();
        // :: error: (lock.not.held)
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
        // :: error: (lock.not.held)
        myField = new MyClass();
        // :: error: (lock.not.held)
        myField.toString();
        // :: error: (lock.not.held)
        this.myField = new MyClass();
        // :: error: (lock.not.held)
        this.myField.toString();
        // :: error: (lock.not.held)
        m.field = new Object();
        // :: error: (lock.not.held)
        m.field.toString();
        // The following error is due to the fact that you cannot access "this.lock" without first
        // having acquired "lock".  The right fix in a user scenario would be to not guard "this"
        // with "this.lock". The current object could instead be guarded by "<self>" or by some
        // other lock expression that is not one of its fields. We are keeping this test case here
        // to make sure this scenario issues a warning.
        // :: error: (lock.not.held)
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
        // :: error: (lock.not.held)
        boolean b4 = compare(p1, myMethod());

        // An error is issued indicating that p2 might be dereferenced without
        // "lock" being held. The method call need not be modified, since
        // @GuardedBy({}) <: @GuardedByUnknown and @GuardedBy("lock") <: @GuardedByUnknown,
        // but the lock must be acquired prior to the method call.
        // :: error: (lock.not.held)
        boolean b2 = compare(p1, p2);
        // :: error: (lock.not.held)
        boolean b3 = compare(p1, this.p2);
        // :: error: (lock.not.held)
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
        // :: error: (lock.not.held)
        Object f = myObj.field;
        // :: error: (lock.not.held)
        Object f2 = myMethodReturningMyObj().field;
        // :: error: (lock.not.held)
        myObj.method(); // method()'s receiver is annotated as @GuardSatisfied
        // :: error: (lock.not.held)
        myMethodReturningMyObj().method(); // method()'s receiver is annotated as @GuardSatisfied

        synchronized (lock) {
            f = myObj.field;
            f2 = myMethodReturningMyObj().field;
            myObj.method();
            myMethodReturningMyObj().method();
        }

        // :: error: (lock.not.held)
        myMethodReturningMyObj().field = new Object();
        // :: error: (lock.not.held)
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
    // :: error: (assignment.type.incompatible)
    @GuardedBy({}) MyClass z = x; // ILLEGAL because dereferences of z do not require "lock" to be held.

    @LockingFree
    @GuardedBy("lock") MyClass myMethod() {
        return x; // OK because the return type is @GuardedBy("lock")
    }

    void exampleMethod() {
        // :: error: (lock.not.held)
        x.field = new Object(); // ILLEGAL because the lock is not known to be held
        // :: error: (lock.not.held)
        y.field = new Object(); // ILLEGAL because the lock is not known to be held
        // :: error: (lock.not.held)
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
        // :: error: (assignment.type.incompatible)
        y5 = x5; // ILLEGAL
    }

    // :: error: (immutable.type.guardedby)
    @GuardedBy("a") String s = "string";

    @GuardedBy({}) MyClass o1;

    @GuardedBy("lock") MyClass o2;

    @GuardedBy("lock") MyClass o3;

    void someMethod() {
        o3 = o2; // OK, since o2 and o3 are guarded by exactly the same lock set.

        // :: error: (assignment.type.incompatible)
        o1 = o2; // Assignment type incompatible errors are issued for both assignments, since
        // :: error: (assignment.type.incompatible)
        o2 = o1; // {"lock"} and {} are not identical sets.
    }

    @SuppressWarnings("lock:cast.unsafe")
    void someMethod2() {
        // A cast can be used if the user knows it is safe to do so.
        // However, the @SuppressWarnings must be added.
        o1 = (@GuardedBy({}) MyClass) o2;
    }

    static final Object myLock = new Object();

    @GuardedBy("ChapterExamples.myLock") MyClass myMethod3() {
        return new MyClass();
    }

    // reassignments without holding the lock are OK.
    @GuardedBy("ChapterExamples.myLock") MyClass x2 = myMethod3();

    @GuardedBy("ChapterExamples.myLock") MyClass y2 = x2;

    void myMethod4() {
        // :: error: (lock.not.held)
        x2.field = new Object(); // ILLEGAL because the lock is not held
        synchronized (ChapterExamples.myLock) {
            y2.field = new Object(); // OK: the lock is held
        }
    }

    void myMethod5(@GuardedBy("ChapterExamples.myLock") MyClass a) {
        // :: error: (lock.not.held)
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
        // :: error: (lock.not.held)
        p2.field = new Object();
        // An error is issued indicating that p2 might be dereferenced without "lock" being held.
        // The method call need not be modified, since @GuardedBy({}) <: @GuardedByUnknown and
        // @GuardedBy("lock") <: @GuardedByUnknown, but the lock must be acquired prior to the
        // method call.
        // :: error: (lock.not.held)
        boolean b2 = compare(p1, p2);
    }

    void helper1(@GuardedBy("ChapterExamples.myLock") MyClass a) {
        // :: error: (lock.not.held)
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
        // :: error: (lock.not.held)
        d.field = new Object(); // ILLEGAL: the lock is not held
    }

    @ReleasesNoLocks
    void helper5() {}
    // No annotation means @ReleasesNoLocks
    void helper6() {}

    void myMethod2(@GuardedBy("ChapterExamples.myLock") MyClass e) {
        helper1(e); // OK to pass to another routine without holding the lock.
        // :: error: (lock.not.held)
        e.field = new Object(); // ILLEGAL: the lock is not held
        // :: error: (contracts.precondition.not.satisfied)
        helper2(e);
        // :: error: (lock.not.held)
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

    // TODO: For now, boxed types are treated as primitive types. This may change in the future.
    @SuppressWarnings("deprecation") // new Integer
    void unboxing() {
        int a = 1;
        // :: error: (immutable.type.guardedby)
        @GuardedBy("lock") Integer c;
        synchronized (lock) {
            // :: error: (assignment.type.incompatible)
            c = a;
        }

        // :: error: (immutable.type.guardedby)
        @GuardedBy("lock") Integer b = 1;
        int d;
        synchronized (lock) {
            d = b;

            // Expected, since b cannot be @GuardedBy("lock") since it is a boxed primitive.
            // :: error: (method.invocation.invalid)
            d = b.intValue(); // The de-sugared version does not issue an error.
        }

        c = c + b; // Syntactic sugar for c = new Integer(c.intValue() + b.intValue()).

        // Expected, since b and c cannot be @GuardedBy("lock") since they are boxed primitives.
        // :: error: (method.invocation.invalid)
        c = new Integer(c.intValue() + b.intValue()); // The de-sugared version

        synchronized (lock) {
            c = c + b; // Syntactic sugar for c = new Integer(c.intValue() + b.intValue()).

            // Expected, since b and c cannot be @GuardedBy("lock") since they are boxed primitives.
            // :: error: (method.invocation.invalid)
            c = new Integer(c.intValue() + b.intValue()); // The de-sugared version
        }

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

      // TODO re-enable this error (lock.not.held)
      c = c + b; // Syntactic sugar for c = new Integer(c.intValue() + b.intValue()), hence 'lock' must be held.
      // TODO re-enable this error (lock.not.held)
      c = new Integer(c.intValue() + b.intValue()); // The de-sugared version

      synchronized(lock) {
        c = c + b; // Syntactic sugar for c = new Integer(c.intValue() + b.intValue()), hence 'lock' must be held.
        c = new Integer(c.intValue() + b.intValue()); // The de-sugared version
      }

      // TODO re-enable this error (lock.not.held)
      a = b; // TODO: This assignment between two reference types should not require a lock to be held.
    }*/

    final ReentrantLock lock1 = new ReentrantLock();
    final ReentrantLock lock2 = new ReentrantLock();

    @GuardedBy("lock1") MyClass filename;

    @GuardedBy("lock2") MyClass extension;

    void method0() {
        // :: error: (lock.not.held) :: error: (lock.not.held)
        filename = filename.append(extension);
    }

    void method1() {
        lock1.lock();
        // :: error: (lock.not.held)
        filename = filename.append(extension);
    }

    void method2() {
        lock2.lock();
        // :: error: (lock.not.held)
        filename = filename.append(extension);
    }

    void method3() {
        lock1.lock();
        lock2.lock();
        filename = filename.append(extension);
        filename = filename.append(null);
        // :: error: (assignment.type.incompatible)
        filename = extension.append(extension);
        // :: error: (assignment.type.incompatible)
        filename = extension.append(filename);
    }

    void matchingGSparams(@GuardSatisfied(1) MyClass m1, @GuardSatisfied(1) MyClass m2) {}

    void method4() {
        lock1.lock();
        lock2.lock();
        matchingGSparams(filename, null);
        matchingGSparams(null, filename);
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
        //    found   : (@org.checkerframework.checker.lock.qual.GuardedBy({}) :: T)[ extends
        // @GuardedByUnknown @LockPossiblyHeld Object super @GuardedBy({}) @LockHeld Void]
        //        required: @GuardedBy @LockPossiblyHeld Object
    }

    private static final Object NULL_KEY = new Object();
    // A guardsatisfied.location.disallowed error is issued for the cast.
    @SuppressWarnings({"cast.unsafe", "guardsatisfied.location.disallowed"})
    private static @GuardSatisfied(1) Object maskNull(@GuardSatisfied(1) Object key) {
        return (key == null ? (@GuardSatisfied(1) Object) NULL_KEY : key);
    }

    public void assignmentOfGSWithNoIndex(@GuardSatisfied Object a, @GuardSatisfied Object b) {
        // :: error: (guardsatisfied.assignment.disallowed)
        a = b;
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
