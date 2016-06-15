import org.checkerframework.checker.lock.qual.*;
import org.checkerframework.checker.nullness.qual.*;
import org.checkerframework.dataflow.qual.*;
import org.checkerframework.framework.qual.ImplicitFor;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedPrimitiveType;

import java.util.concurrent.locks.ReentrantLock;
import java.util.*;

class ItselfExpressionCases {
    final Object somelock = new Object();

    final private @GuardedBy({"<self>"}) MyClass m = new MyClass();
    @Pure
    private @GuardedBy({"<self>"}) MyClass getm() {
        return m;
    }
    @Pure
    private @GuardedBy({"<self>"}) MyClass getm2(@GuardedBy("<self>") ItselfExpressionCases this) {
        // The following error is due to the precondition of the this.m field dereference not being satisfied.
        //:: error: (contracts.precondition.not.satisfied.field)
        return m;
    }

    @Pure
    private Object getmfield() {
        //:: error: (contracts.precondition.not.satisfied)
        return getm().field;
    }

    public void arrayTest(final Object @GuardedBy("<self>") [] a1) {
        //:: error: (contracts.precondition.not.satisfied.field)
        Object a = a1[0];
        synchronized(a1) {
            a = a1[0];
        }
    }

    Object @GuardedBy("<self>") [] a2;

    @Pure
    public Object @GuardedBy("<self>") [] geta2() {
        return a2;
    }

    public void arrayTest() {
        //:: error: (contracts.precondition.not.satisfied)
        Object a = geta2()[0];
        synchronized(geta2()) {
            a = geta2()[0];
        }
    }

    public void testCheckPreconditions(final @GuardedBy("<self>") MyClass o, @GuardSatisfied Object gs, @GuardSatisfied MyClass gsMyClass) {
        //:: error: (contracts.precondition.not.satisfied)
        getm().field = new Object();
        synchronized(getm()) {
            getm().field = new Object();
        }

        //:: error: (contracts.precondition.not.satisfied.field)
        m.field = new Object();
        synchronized(m) {
            m.field = new Object();
        }

        //:: error: (contracts.precondition.not.satisfied.field)
        gs = m.field;
        synchronized(m) {
            gs = m.field;
        }

        //:: error: (contracts.precondition.not.satisfied)
        gs = getm().field;
        synchronized(getm()) {
            gs = getm().field;
        }

        //:: error: (contracts.precondition.not.satisfied)
        gsMyClass = getm();
        synchronized(getm()) {
            gsMyClass = getm();
        }

        //:: error: (contracts.precondition.not.satisfied)
        o.foo();
        synchronized(o) {
            //:: error: (contracts.precondition.not.satisfied)
            o.foo();
            synchronized(somelock) {
                o.foo();
            }
        }

        //:: error: (contracts.precondition.not.satisfied)
        o.foo2();
        synchronized(o) {
            o.foo2();
        }
    }

    class MyClass {
        Object field = new Object();

        @Holding("somelock")
        void foo(@GuardSatisfied MyClass this) {}

        void foo2(@GuardSatisfied MyClass this) {}

        void method(@GuardedBy("<self>") MyClass this) {
            //:: error: (contracts.precondition.not.satisfied)
            this.foo();
            //:: error: (contracts.precondition.not.satisfied)
            foo();
            synchronized(somelock) {
                //:: error: (contracts.precondition.not.satisfied)
                this.foo();
                //:: error: (contracts.precondition.not.satisfied)
                foo();
                synchronized(this) {
                    this.foo();
                    foo();
                }
            }

            //:: error: (contracts.precondition.not.satisfied)
            this.foo2();
            //:: error: (contracts.precondition.not.satisfied)
            foo2();
            synchronized(this) {
                this.foo2();
                foo2();
            }
        }
    }
}
