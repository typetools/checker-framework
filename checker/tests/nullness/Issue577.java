// Test case for issue #577 with expected errors:
// Also see test case in framework/tests/all-systems/Issue577.java
// https://github.com/typetools/checker-framework/issues/577

import org.checkerframework.checker.nullness.qual.*;

class Banana<T extends Number> extends Apple<int[]> {
  @Override
  void fooOuter(int[] array) {}

  class InnerBanana extends InnerApple<long[]> {
    @Override
    // :: error: (override.param.invalid)
    <F2 extends Object> void foo(int[] array, long[] array2, F2 param3) {}
  }
}

class Apple<T> {
  void fooOuter(T param) {}

  class InnerApple<E> {
    <F> void foo(T param, E param2, F param3) {}
  }
}

class Pineapple<E extends Object> extends Apple<E> {
  @Override
  void fooOuter(E array) {}

  class InnerPineapple extends InnerApple<@Nullable String> {
    @Override
    // :: error: (override.param.invalid)
    <F3> void foo(E array, String array2, F3 param3) {}
  }
}

class IntersectionAsMemberOf {
  interface MyGenericInterface<F> {
    F getF();
  }

  <T extends Object & MyGenericInterface<@NonNull String>> void foo(T param) {
    @NonNull String s = param.getF();
  }
}

class UnionAsMemberOf {
  interface MyInterface<T> {
    T getT();
  }

  class MyExceptionA extends Throwable implements Cloneable, MyInterface<@NonNull String> {
    public String getT() {
      return "t";
    }
  }

  class MyExceptionB extends Throwable implements Cloneable, MyInterface<String> {
    public String getT() {
      return "t";
    }
  }

  void bar() throws MyExceptionA, MyExceptionB {}

  void foo1(MyInterface<Throwable> param) throws Throwable {
    try {
      bar();
    } catch (MyExceptionA | MyExceptionB ex1) {
      @NonNull String s = ex1.getT();
    }
  }
}
