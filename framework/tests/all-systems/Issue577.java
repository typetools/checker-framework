// Test case for issue #577:
// Test with expected errors: checker/tests/nullness/Issue577.java
// https://github.com/typetools/checker-framework/issues/577
class Banana<T extends Number> extends Apple<int[]> {
  @Override
  void fooIssue577Outer(int[] array) {}

  class InnerBanana extends InnerApple<long[]> {
    @Override
    <F2> void foo(int[] array, long[] array2, F2 param3) {}
  }
}

class Apple<T> {
  void fooIssue577Outer(T param) {}

  class InnerApple<E> {
    <F> void foo(T param, E param2, F param3) {}
  }
}

class Pineapple<E> extends Apple<E> {
  @Override
  void fooIssue577Outer(E array) {}
}

class IntersectionAsMemberOf {
  interface MyGenericInterface<F> {
    F getF();
  }

  <T extends Object & MyGenericInterface<String>> void foo(T param) {
    String s = param.getF();
  }
}

class UnionAsMemberOf {
  interface MyInterface<T> {
    T getT();
  }

  class MyExceptionA extends Throwable implements Cloneable, MyInterface<String> {
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
      String s = ex1.getT();
    }
  }
}

final class Issue577Outer<K extends Object> {
  private Inner createInner(ReferenceQueue<? super K> q) {
    return new Inner(q);
  }

  private final class Inner {
    private Inner(ReferenceQueue<? super K> q) {}
  }
}

class ReferenceQueue<T> {}
