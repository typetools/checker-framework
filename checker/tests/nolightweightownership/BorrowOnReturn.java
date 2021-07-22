// tests that the Must Call checker respects the Owning and NotOwning annotations on return values.

import org.checkerframework.checker.mustcall.qual.*;

class BorrowOnReturn {
    @MustCall("a") class Foo {
        void a() {}
    }

    @Owning
    Object getOwnedFoo() {
        // :: error: return
        return new Foo();
    }

    Object getNoAnnoFoo() {
        // Treat as owning, so warn
        // :: error: return
        return new Foo();
    }

    @NotOwning
    Object getNotOwningFooWrong() {
        // :: error: return
        return new Foo();
    }

    Object getNotOwningFooRightButNoNotOwningAnno() {
        Foo f = new Foo();
        f.a();
        // This is still an error for now, because it's treated as an owning pointer. TODO: fix this
        // kind of FP?
        // :: error: return
        return f;
    }

    @NotOwning
    Object getNotOwningFooRight() {
        Foo f = new Foo();
        f.a();
        // :: error: return
        return f;
    }

    @MustCall("a") Object getNotOwningFooRight2() {
        return new Foo();
    }
}
