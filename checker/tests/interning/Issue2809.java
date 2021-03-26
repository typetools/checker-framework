// test cases for #2809
// https://github.com/typetools/checker-framework/issues/2809

import org.checkerframework.checker.interning.qual.Interned;
import org.checkerframework.checker.interning.qual.UnknownInterned;

public class Issue2809 {

  void new1(MyType<int @Interned []> t, int @Interned [] non) {
    t.self(new MyType<>(non));
  }

  void new2(MyType<int @Interned []> t, int @Interned [] non) {
    t.self(new MyType<int @Interned []>(non));
  }

  void new3(MyType<@Interned MyType<Object>> t, @Interned MyType<Object> non) {
    t.self(new MyType<>(non));
  }

  void newFail(MyType<int @Interned []> t, int @UnknownInterned [] non) {
    // :: error: (argument.type.incompatible)
    t.self(new MyType<>(non));
  }

  class MyType<T> {
    MyType(T p) {}

    void self(MyType<T> myType) {}
  }
}
