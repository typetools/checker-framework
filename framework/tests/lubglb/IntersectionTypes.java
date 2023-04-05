import org.checkerframework.framework.testchecker.lubglb.quals.*;

interface Foo {}

interface Bar {}

class Baz implements Foo, Bar {}

public class IntersectionTypes {
  // :: warning: (explicit.annotation.ignored)
  <S extends @LubglbB Foo & @LubglbC Bar> void call1(S p) {}

  // :: warning: (explicit.annotation.ignored)
  <T extends @LubglbC Bar & @LubglbB Foo> void call2(T p) {}

  void foo1(@LubglbD Baz baz1) {
    call1(baz1);
    call2(baz1);
  }

  void foo2(@LubglbF Baz baz2) {
    call1(baz2);
    call2(baz2);
  }

  void foo3(@LubglbB Baz baz3) {
    call1(baz3);
    // :: error: (type.arguments.not.inferred)
    call2(baz3);
  }
}
