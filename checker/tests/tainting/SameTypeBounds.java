package wildcards;

import org.checkerframework.checker.tainting.qual.Tainted;
import org.checkerframework.checker.tainting.qual.Untainted;

public class SameTypeBounds {
  static class MyGen<T> {}

  void test1(MyGen<Object> p) {
    // TODO: error: invalid type
    // The upper and lower bound must have the same annotation because the bounds are collasped
    // during capture conversion.
    MyGen<? super Object> o = p;
    // :: error: (assignment)
    p = o;
  }

  void test2(MyGen<Object> p) {
    // :: error: (assignment)
    MyGen<@Untainted ? super @Untainted Object> o = p;
    // :: error: (assignment)
    p = o;
  }

  void test3(MyGen<@Untainted Object> p) {
    // :: error: (assignment)
    MyGen<? super @Tainted Object> o = p;
    // :: error: (assignment)
    p = o;
  }

  static class MyClass {}

  static class MySubClass extends MyClass {}

  class Gen<E extends MyClass> {}

  void test3(Gen<MyClass> p, Gen<? super MyClass> p2) {
    // TODO: error: invalid type
    Gen<? super MyClass> o = p;
    o = p2;
    // :: error: (assignment)
    p = p2;
  }
}
