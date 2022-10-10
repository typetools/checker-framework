// test case for https://github.com/kelloggm/object-construction-checker/issues/366

import org.checkerframework.checker.mustcall.qual.*;

class Enclosing {
    @InheritableMustCall("a")
    static class Foo {
        void a() {}
    }

    static class Nested {
        // :: error: required.method.not.called
        @Owning Foo foo;

        @CreatesMustCallFor("this")
        // :: error: creates.mustcall.for.invalid.target
        void initFoo() {
            if (this.foo == null) {
                this.foo = new Foo();
            }
        }
    }
}
