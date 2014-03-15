// Test case for Issue 247:
// https://code.google.com/p/checker-framework/issues/detail?id=247

interface Foo {}
interface Bar {}
class Baz implements Foo, Bar {}

class IntersectionTypes {
  void foo() {
    Baz baz = new Baz();
    call(baz);
  }

  <T extends Foo & Bar> void call(T p) {}
}
