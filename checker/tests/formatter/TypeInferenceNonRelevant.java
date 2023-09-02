public class TypeInferenceNonRelevant {

  static class MyClass implements Interface<MyClass> {

    MyClass returnType;

    void method(MyClass result) {
      result.returnType = Interface.method(this.returnType);
    }
  }

  interface Interface<T> {
    static <T2 extends Interface<T2>> T2 method(T2 object) {
      throw new RuntimeException();
    }
  }
}
