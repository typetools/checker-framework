public class Unions {
  void foo1(MyInterface<Throwable> param) throws Throwable {
    try {
      bar();
    } catch (MyExceptionA | MyExceptionB ex) {
      typeVar(ex);
      typeVarIntersection(ex);

      typeVarWildcard(ex, param);
      typeVarWildcard2(ex, param);
    }
  }

  void foo2(MyInterface<Throwable> param) throws Throwable {
    try {
      bar();
    } catch (SubMyExceptionA | SubMyExceptionA2 ex) {
      typeVar(ex);
      typeVar2(ex, ex);

      typeVarIntersection(ex);

      typeVarWildcard(ex, param);
      typeVarWildcard2(ex, param);
    }
  }

  <T extends Cloneable & MyInterface<String>> void typeVarIntersection(T param) {}

  <T extends Throwable> void typeVar(T param) {}

  <T extends Throwable> void typeVar2(T param, T param2) {}

  <T extends Throwable> void typeVarWildcard(T param, MyInterface<? extends T> myInterface) {}

  <T extends Throwable> void typeVarWildcard2(T param, MyInterface<? super T> myInterface) {}

  void bar() throws MyExceptionA, MyExceptionB {}

  interface MyInterface<T> {}

  class MyExceptionA extends Throwable implements Cloneable, MyInterface<String> {}

  class MyExceptionB extends Throwable implements Cloneable, MyInterface<String> {}

  class SubMyExceptionA extends MyExceptionA {}

  class SubMyExceptionA2 extends MyExceptionA {}
}
