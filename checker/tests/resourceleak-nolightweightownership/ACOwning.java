// This copy of the ACOwning.java test from the mustcall tests
// has expected errors as if ownership transfer can only happen based
// on defaults - that is, that @Owning and @NotOwning annotations are
// ignored.

import org.checkerframework.checker.mustcall.qual.*;
import org.checkerframework.common.returnsreceiver.qual.*;

class ACOwning {

    @MustCall("a") static class Foo {
        void a() {}
    }

    Foo makeFoo() {
        return new Foo();
    }

    static void takeOwnership(@Owning Foo foo) {
        foo.a();
    }

    static void noOwnership(Foo foo) {}

    static void takeOwnershipWrong(@Owning Foo foo) {}

    static @NotOwning Foo getNonOwningFoo() {
        return new Foo();
    }

    static void callGetNonOwningFoo() {
        // :: error: required.method.not.called
        getNonOwningFoo();
    }

    static void ownershipInCallee() {
        // :: error: required.method.not.called
        Foo f = new Foo();
        takeOwnership(f);
        // :: error: required.method.not.called
        Foo g = new Foo();
        noOwnership(g);
    }

    @Owning
    public Foo owningAtReturn() {
        return new Foo();
    }

    void owningAtReturnTest() {
        // :: error: required.method.not.called
        Foo f = owningAtReturn();
    }

    void ownershipTest() {
        // :: error: required.method.not.called
        takeOwnership(new Foo());
    }
}
