// Test case for Issue 247:
// https://github.com/typetools/checker-framework/issues/247

interface Foo {}

interface Bar {}

class Baz implements Foo, Bar {}

public class IntersectionTypes {
    void foo() {
        Baz baz = new Baz();
        call(baz);
    }

    <T extends Foo & Bar> void call(T p) {}
}
