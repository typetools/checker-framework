import org.checkerframework.checker.interning.qual.Interned;

public class ExpressionsInterning {

    class A {
        B b;
    }

    class B {
        C c;

        D d() {
            return new D();
        }

        Boolean bBoolean() {
            return true;
        }
    }

    class C {}

    class D {}

    public Boolean fieldThenMethod(A a) {
        Boolean temp = a.b.bBoolean();
        return temp;
    }

    class Foo {
        public @Interned Foo returnThis(@Interned Foo this, @Interned Foo other) {
            if (other == this) {
                return this;
            } else {
                return null;
            }
        }
    }

    // :: warning: (cast.unsafe.constructor.invocation)
    public @Interned Foo THEONE = new @Interned Foo();

    public boolean isItTheOne(Foo f) {
        return THEONE.equals(f);
    }

    // A warning when interned objects are compared via .equals helps me in determining whether it
    // is a good idea to convert a given class or reference to @Interned -- I can see whether there
    // are places that it is compared with .equals, which I might need to examine.
    public boolean dontUseEqualsMethod(@Interned Foo f1, @Interned Foo f2) {
        // :: warning: (unnecessary.equals)
        return f1.equals(f2);
    }
}
