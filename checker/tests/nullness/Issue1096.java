// Test case for Issue 1096:
// https://github.com/typetools/checker-framework/issues/1027

import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.Raw;
import org.checkerframework.checker.nullness.qual.RequiresNonNull;

class PreCond {
    Object f;
    @Nullable Object g;

    PreCond() {
        f = new Object();
        early();
        g = new Object();
        doNullable();
    }

    void earlyBad(@UnknownInitialization @Raw PreCond this) {
        //:: error: (dereference.of.nullable)
        f.toString();
    }

    @RequiresNonNull("this.f")
    void early(@UnknownInitialization @Raw PreCond this) {
        f.toString();
    }

    @RequiresNonNull("this.g")
    void doNullable(@UnknownInitialization @Raw PreCond this) {
        g.toString();
    }

    void foo(@UnknownInitialization @Raw PreCond this) {
        // Receiver is not fully initialized, so raise error
        //:: error: (contracts.precondition.not.satisfied)
        early();
    }

    void bar() {
        // Receiver is initialized, so non-null field f is definitely non-null
        early();
        // Nullable fields stay nullable
        //:: error: (contracts.precondition.not.satisfied)
        doNullable();
    }
}

class User {
    void foo(@UnknownInitialization @Raw PreCond pc) {
        // Receiver is not fully initialized, so raise error
        //:: error: (contracts.precondition.not.satisfied)
        pc.early();
    }

    void bar(PreCond pc) {
        // Receiver is initialized, so non-null field f is definitely non-null
        pc.early();
        // Nullable fields stay nullable
        //:: error: (contracts.precondition.not.satisfied)
        pc.doNullable();
    }
}
