import org.checkerframework.checker.mustcall.qual.*;
import org.checkerframework.checker.nullness.qual.*;

class NotCheckingDeadCode {

  static class Foo {}

  static Foo makeFoo() {
    return new Foo();
  }

  static Foo makeFoo1() {
    throw new UnsupportedOperationException();
  }

  static Foo makeFoo2() throws UnsupportedOperationException {
    return new Foo();
  }

  static Foo makeFoo3() throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  Foo fooField;

  void test1() {
    try {
      fooField = makeFoo();
    } catch (Exception e) {
      Foo f = null;
      fooField = f;
    }
  }

  void test2() {
    try {
      fooField = makeFoo1();
    } catch (Exception e) {
      Foo f = null;
      fooField = f;
    }
  }

  void test3() {
    Foo f = null;
    try {
      fooField = makeFoo1();
    } catch (Exception e) {
      fooField = f;
    }
  }

  void test4() {
    try {
      fooField = makeFoo2();
    } catch (Exception e) {
      Foo f = null;
      fooField = f;
    }
  }

  void test5() {
    try {
      fooField = makeFoo3();
    } catch (Exception e) {
      Foo f = null;
      fooField = f;
    }
  }

  void test6() {
    Foo f = null;
    try {
      fooField = makeFoo3();
    } catch (Exception e) {
      fooField = f;
    }
  }
}
