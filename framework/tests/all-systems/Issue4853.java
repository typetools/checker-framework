@SuppressWarnings("all") // Just check for crashes.
public class Issue4853 {
  interface Interface<T> {}

  static class MyClass<T> {
    class InnerMyClass implements Interface<T> {}
  }

  abstract static class SubMyClass extends MyClass<Void> {
    protected void f() {
      method(new InnerMyClass());
    }

    abstract void method(Interface<Void> callback);
  }
}
