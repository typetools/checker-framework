public class Issue3791 {
  interface MyInterface {}

  abstract static class MyClass<A extends MyInterface> {}

  static class SubMyClass<B> extends MyClass<Generic<? extends B>> {}

  static class Generic<C> implements MyInterface {}

  abstract static class MyInterfaceMyClass<D extends MyInterface, E extends MyClass<? super D>> {}

  void method(MyInterfaceMyClass<?, ?> param) {
    @SuppressWarnings("unchecked")
    MyInterfaceMyClass<?, SubMyClass<?>> local = (MyInterfaceMyClass<?, SubMyClass<?>>) param;
  }
}
