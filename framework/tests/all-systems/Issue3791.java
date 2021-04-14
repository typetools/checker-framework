public class Issue3791 {
  interface MyInterface {}

  abstract static class MyClass<A extends MyInterface> {}

  static class SubMyClass<B> extends MyClass<Generic<? extends B>> {}

  static class Generic<C> implements MyInterface {}

  abstract static class MyInterfaceMyClass<D extends MyInterface, E extends MyClass<? super D>> {}

  void method(MyInterfaceMyClass<?, ?> param) {
    // TODO: Should we open an issue for this?
    // See code is reject by Eclipse and should be reject by javac.
    @SuppressWarnings({"unchecked", "type.argument.type.incompatible"})
    MyInterfaceMyClass<?, SubMyClass<?>> local = (MyInterfaceMyClass<?, SubMyClass<?>>) param;
  }
}
