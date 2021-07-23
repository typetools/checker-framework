import org.checkerframework.checker.lock.qual.GuardedBy;
import org.checkerframework.checker.lock.qual.GuardedByUnknown;
import org.checkerframework.checker.lock.qual.MayReleaseLocks;
import org.checkerframework.dataflow.qual.Deterministic;
import org.checkerframework.dataflow.qual.Pure;

import java.util.concurrent.locks.ReentrantLock;

public class LockExpressionIsFinal {

    class MyClass {
        Object field = new Object();
    }

    class C1 {
        final C1 field =
                new C1(); // Infinite loop. This code is not meant to be executed, only type
        // checked.
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

    // Analogous to testExplicitLockExpressionIsFinal and testGuardedByExpressionIsFinal, but for
    // monitor locks acquired in synchronized blocks.
    void testSynchronizedExpressionIsFinal(boolean b) {
        synchronized (c1) {
        }

        Object o1 = new Object(); // o1 is effectively final - it is never reassigned
        Object o2 = new Object(); // o2 is reassigned later - it is not effectively final
        synchronized (o1) {
        }
        // :: error: (lock.expression.not.final)
        synchronized (o2) {
        }

        o2 = new Object(); // Reassignment that makes o2 not have been effectively final earlier.

        // Tests that package names are considered final.
        synchronized (java.lang.String.class) {
        }

        // Test a tree that is not supported by LockVisitor.ensureExpressionIsEffectivelyFinal
        // :: error: (lock.expression.possibly.not.final)
        synchronized (c1.getFieldPure(b ? c1 : o1, c1)) {
        }

        synchronized (
                c1.field.field.field.getFieldPure(
                                c1.field, c1.getFieldDeterministic().getFieldPure(c1, c1.field))
                        .field) {
        }

        // The following negative test cases are the same as the one above but with one modification
        // in each.

        synchronized (
                // :: error: (lock.expression.not.final)
                c1.field.field2.field.getFieldPure(
                                c1.field, c1.getFieldDeterministic().getFieldPure(c1, c1.field))
                        .field) {
        }
        synchronized (
                c1.field.field.field.getFieldPure(
                                // :: error: (lock.expression.not.final)
                                c1.field, c1.getField().getFieldPure(c1, c1.field))
                        .field) {
        }
    }

    class C2 extends ReentrantLock {
        final C2 field =
                new C2(); // Infinite loop. The code is not meant to be executed, only type-checked.
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

    // Analogous to testSynchronizedExpressionIsFinal and testGuardedByExpressionIsFinal, but for
    // explicit locks.
    @MayReleaseLocks
    void testExplicitLockExpressionIsFinal(boolean b) {
        c2.lock();

        ReentrantLock rl1 =
                new ReentrantLock(); // rl1 is effectively final - it is never reassigned
        ReentrantLock rl2 =
                new ReentrantLock(); // rl2 is reassigned later - it is not effectively final
        rl1.lock();
        rl1.unlock();
        // :: error: (lock.expression.not.final)
        rl2.lock();
        // :: error: (lock.expression.not.final)
        rl2.unlock();

        rl2 = new ReentrantLock(); // Reassignment that makes rl2 not have been effectively final
        // earlier.

        // Test a tree that is not supported by LockVisitor.ensureExpressionIsEffectivelyFinal
        // :: error: (lock.expression.possibly.not.final)
        c2.getFieldPure(b ? c2 : rl1, c2).lock();
        // :: error: (lock.expression.possibly.not.final)
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

        // The following negative test cases are the same as the one above but with one modification
        // in each.

        c2.field
                // :: error: (lock.expression.not.final)
                .field2
                .field
                .getFieldPure(c2.field, c2.getFieldDeterministic().getFieldPure(c2, c2.field))
                .field
                .lock();
        c2.field
                // :: error: (lock.expression.not.final)
                .field2
                .field
                .getFieldPure(c2.field, c2.getFieldDeterministic().getFieldPure(c2, c2.field))
                .field
                .unlock();

        c2.field
                .field
                .field
                // :: error: (lock.expression.not.final)
                .getFieldPure(c2.field, c2.getField().getFieldPure(c2, c2.field))
                .field
                .lock();
        c2.field
                .field
                .field
                // :: error: (lock.expression.not.final)
                .getFieldPure(c2.field, c2.getField().getFieldPure(c2, c2.field))
                .field
                .unlock();
    }

    // Analogous to testSynchronizedExpressionIsFinal and testExplicitLockExpressionIsFinal, but for
    // expressions in @GuardedBy annotations.
    void testGuardedByExpressionIsFinal() {
        @GuardedBy("c1") Object guarded1;

        final Object o1 = new Object();
        Object o2 = new Object();
        // reassign so it's not effectively final
        o2 = new Object();

        @GuardedBy("o1") Object guarded2 = new Object();
        // :: error: (lock.expression.not.final)
        @GuardedBy("o2") Object guarded3 = new Object();

        // Test expressions that are not supported by LockVisitor.ensureExpressionIsEffectivelyFinal
        @GuardedBy("java.lang.String.class") Object guarded4;
        // :: error: (expression.unparsable.type.invalid)
        @GuardedBy("c1.getFieldPure(b ? c1 : o1, c1)") Object guarded5;

        @GuardedBy(
                "c1.field.field.field.getFieldPure"
                        + "(c1.field, c1.getFieldDeterministic().getFieldPure(c1, c1.field)).field")
        Object guarded6;

        @GuardedBy("c1.field.field.field.getFieldPure2().getFieldDeterministic().field") Object guarded7;

        // The following negative test cases are the same as the one above but with one modification
        // in each.

        // :: error: (lock.expression.not.final)
        @GuardedBy("c1.field.field2.field.getFieldPure2().getFieldDeterministic().field") Object guarded8;
        // :: error: (lock.expression.not.final)
        @GuardedBy("c1.field.field.field.getField().getFieldDeterministic().field") Object guarded9;

        // Additional test cases to test that method parameters (in this case the parameters to
        // getFieldPure) are parsed.
        @GuardedBy("c1.field.field.field.getFieldPure(c1, c1).getFieldDeterministic().field") Object guarded10;
        @GuardedBy("c1.field.field.field.getFieldPure(c1, o1).getFieldDeterministic().field") Object guarded11;
        // :: error: (lock.expression.not.final)
        @GuardedBy("c1.field.field.field.getFieldPure(c1, o2).getFieldDeterministic().field") Object guarded12;

        // Test that @GuardedBy annotations on various tree kinds inside a method are visited

        Object guarded13 = (@GuardedBy("o1") Object) guarded2;
        // :: error: (lock.expression.not.final)
        Object guarded14 = (@GuardedBy("o2") Object) guarded3;

        @GuardedBy("o1") Object guarded15[] = new @GuardedBy("o1") MyClass[3];
        // :: error: (lock.expression.not.final)
        @GuardedBy("o2") Object guarded16[] = new @GuardedBy("o2") MyClass[3];

        // Tests that the location of the @GB annotation inside a VariableTree does not matter (i.e.
        // it does not need to be the leftmost subtree).
        Object guarded17 @GuardedBy("o1") [];
        // :: error: (lock.expression.not.final)
        Object guarded18 @GuardedBy("o2") [];

        @GuardedBy("o1") Object guarded19[];
        // :: error: (lock.expression.not.final)
        @GuardedBy("o2") Object guarded20[];

        MyParameterizedClass1<@GuardedBy("o1") Object> m1;
        // :: error: (lock.expression.not.final)
        MyParameterizedClass1<@GuardedBy("o2") Object> m2;

        boolean b = c1 instanceof @GuardedBy("o1") Object;
        // instanceof expression have not effect on the type.
        // // :: error: (lock.expression.not.final)
        b = c1 instanceof @GuardedBy("o2") Object;

        // Additional tests just outside of this method below:
    }

    // Test that @GuardedBy annotations on various tree kinds outside a method are visited

    // Test that @GuardedBy annotations on method return types are visited. No need to test method
    // receivers and parameters as they are covered by tests above that visit VariableTree.

    final Object finalField = new Object();
    Object nonFinalField = new Object();

    @GuardedBy("finalField") Object testGuardedByExprIsFinal1() {
        return null;
    }

    // :: error: (lock.expression.not.final)
    @GuardedBy("nonFinalField") Object testGuardedByExprIsFinal2() {
        return null;
    }

    <T extends @GuardedBy("finalField") Object> T myMethodThatReturnsT_1(T t) {
        return t;
    }

    // :: error: (lock.expression.not.final)
    <T extends @GuardedBy("nonFinalField") Object> T myMethodThatReturnsT_2(T t) {
        return t;
    }

    class MyParameterizedClass1<T extends @GuardedByUnknown Object> {}

    MyParameterizedClass1<? super @GuardedBy("finalField") Object> m1;
    // :: error: (lock.expression.not.final)
    MyParameterizedClass1<? super @GuardedBy("nonFinalField") Object> m2;

    MyParameterizedClass1<? extends @GuardedBy("finalField") Object> m3;
    // :: error: (lock.expression.not.final)
    MyParameterizedClass1<? extends @GuardedBy("nonFinalField") Object> m4;

    class MyClassContainingALock {
        final ReentrantLock finalLock = new ReentrantLock();
        ReentrantLock nonFinalLock = new ReentrantLock();
        Object field;
    }

    void testItselfFinalLock() {
        @SuppressWarnings("assignment") // prevent flow-sensitive type refinement
        final @GuardedBy("<self>.finalLock") MyClassContainingALock m = someValue();
        // :: error: (lock.not.held)
        m.field = new Object();
        // Ignore this error: it is expected that an error will be issued for dereferencing 'm' in
        // order to take the 'm.finalLock' lock.  Typically, the Lock Checker does not support an
        // object being guarded by one of its fields, but this is sometimes done in user code with a
        // ReentrantLock field guarding its containing object. This unfortunately makes it a bit
        // difficult for users since they have to add a @SuppressWarnings for this call while still
        // making sure that warnings for other dereferences are not suppressed.
        // :: error: (lock.not.held)
        m.finalLock.lock();
        m.field = new Object();
    }

    void testItselfNonFinalLock() {
        @SuppressWarnings("assignment") // prevent flow-sensitive type refinement
        final @GuardedBy("<self>.nonFinalLock") MyClassContainingALock m = someValue();
        // ::error: (lock.not.held) :: error: (lock.expression.not.final)
        m.field = new Object();
        // ::error: (lock.not.held) :: error: (lock.expression.not.final)
        m.nonFinalLock.lock();
        // :: error: (lock.expression.not.final)
        m.field = new Object();
    }

    @GuardedByUnknown MyClassContainingALock someValue() {
        return new MyClassContainingALock();
    }
}
