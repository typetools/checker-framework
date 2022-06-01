public class MultipleUnions {
  public static boolean flag = false;

  @SuppressWarnings("ainfertest") // only check WPI for crashes
  void foo1(MyInterface<Throwable> param) throws Throwable {
    try {
      bar();
    } catch (MyExceptionA | MyExceptionB ex1) {
      try {
        bar();
      } catch (SubMyExceptionA | MyExceptionB ex2) {

        Throwable t = flag ? ex1 : ex2;
        typeVar(ex1, ex1);
        typeVar(ex2, ex2);
        // See UnionCrash for version that crashes
        // typeVar(ex1, ex2);
      }
    }
  }

  <T extends Cloneable & MyInterface<String>> void typeVarIntersection(T param) {}

  <T extends Throwable> void typeVar(T param, T param2) {}

  <T extends Throwable> void typeVarWildcard(T param, MyInterface<? extends T> myInterface) {}

  <T extends Throwable> void typeVarWildcard2(T param, MyInterface<? super T> myInterface) {}

  void bar() throws MyExceptionA, MyExceptionB {}

  interface MyInterface<T> {}

  class MyExceptionA extends Throwable implements Cloneable, MyInterface<String> {}

  class MyExceptionB extends Throwable implements Cloneable, MyInterface<String> {}

  class SubMyExceptionA extends MyExceptionA {}
}
