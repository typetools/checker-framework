import org.checkerframework.checker.lock.qual.*;
import org.checkerframework.checker.nullness.qual.*;
import org.checkerframework.dataflow.qual.*;

public class ItselfExpressionCases {
    final Object somelock = new Object();

    private final @GuardedBy({"<self>"}) MyClass m = new MyClass();

    @Pure
    private @GuardedBy({"<self>"}) MyClass getm() {
        return m;
    }

    @Pure
    private @GuardedBy({"<self>"}) MyClass getm2(@GuardedBy("<self>") ItselfExpressionCases this) {
        // The following error is due to the precondition of the this.m field dereference not being
        // satisfied.
        // :: error: (lock.not.held)
        return m;
    }

    @Pure
    private Object getmfield() {
        // :: error: (lock.not.held)
        return getm().field;
    }

    public void arrayTest(final Object @GuardedBy("<self>") [] a1) {
        // :: error: (lock.not.held)
        Object a = a1[0];
        synchronized (a1) {
            a = a1[0];
        }
    }

    Object @GuardedBy("<self>") [] a2;

    @Pure
    public Object @GuardedBy("<self>") [] geta2() {
        return a2;
    }

    public void arrayTest() {
        // :: error: (lock.not.held)
        Object a = geta2()[0];
        synchronized (geta2()) {
            a = geta2()[0];
        }
    }

    public void testCheckPreconditions(
            final @GuardedBy("<self>") MyClass o,
            @GuardSatisfied Object gs,
            @GuardSatisfied MyClass gsMyClass) {
        // :: error: (lock.not.held)
        getm().field = new Object();
        synchronized (getm()) {
            getm().field = new Object();
        }

        // :: error: (lock.not.held)
        m.field = new Object();
        synchronized (m) {
            m.field = new Object();
        }

        // :: error: (lock.not.held)
        gs = m.field;
        synchronized (m) {
            gs = m.field;
        }

        // :: error: (lock.not.held)
        gs = getm().field;
        synchronized (getm()) {
            gs = getm().field;
        }

        // :: error: (lock.not.held)
        gsMyClass = getm();
        synchronized (getm()) {
            gsMyClass = getm();
        }

        // :: error: (lock.not.held) :: error: (flowexpr.parse.error)
        o.foo();
        synchronized (o) {
            // :: error: (flowexpr.parse.error)
            o.foo();
            synchronized (somelock) {
                // o.foo() requires o.somelock is held, not this.somelock.
                // :: error: (flowexpr.parse.error)
                o.foo();
            }
        }

        // :: error: (lock.not.held)
        o.foo2();
        synchronized (o) {
            o.foo2();
        }
    }

    class MyClass {
        Object field = new Object();

        @Holding("somelock")
        void foo(@GuardSatisfied MyClass this) {}

        void foo2(@GuardSatisfied MyClass this) {}

        void method(@GuardedBy("<self>") MyClass this) {
            // :: error: (lock.not.held) :: error: (contracts.precondition.not.satisfied)
            this.foo();
            // :: error: (lock.not.held):: error: (contracts.precondition.not.satisfied)
            foo();
            // :: error: (lock.not.held)
            synchronized (somelock) {
                // :: error: (lock.not.held)
                this.foo();
                // :: error: (lock.not.held)
                foo();
                synchronized (this) {
                    this.foo();
                    foo();
                }
            }

            // :: error: (lock.not.held)
            this.foo2();
            // :: error: (lock.not.held)
            foo2();
            synchronized (this) {
                this.foo2();
                foo2();
            }
        }
    }
}
