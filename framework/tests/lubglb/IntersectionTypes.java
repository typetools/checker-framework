import org.checkerframework.framework.testchecker.lubglb.quals.*;

interface Foo {}

interface Bar {}

class Baz implements Foo, Bar {}

class IntersectionTypes {
    // :: warning: (explicit.annotation.ignored)
    <S extends @B Foo & @C Bar> void call1(S p) {}

    // :: warning: (explicit.annotation.ignored)
    <T extends @C Bar & @B Foo> void call2(T p) {}

    void foo1(@D Baz baz1) {
        call1(baz1);
        call2(baz1);
    }

    void foo2(@F Baz baz2) {
        call1(baz2);
        call2(baz2);
    }

    void foo3(@B Baz baz3) {
        call1(baz3);
        // :: error: (type.argument.type.incompatible)
        call2(baz3);
    }
}
