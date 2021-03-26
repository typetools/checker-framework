// Test case for Issue 1442.
// https://github.com/typetools/checker-framework/issues/1442

public class Issue1442 {
  protected void configure(SubConfig<SubMyClass>.SubConfigInner x) {
    SubMyClass subMyClass = x.getT().getSubConfigInner().outerClassTypeVar();
  }

  static class MyClass<A extends MyClass<A>> {}

  static class SubMyClass extends MyClass<SubMyClass> {}

  static class Config<B extends MyClass<B>> {
    class ConfigInner<T> {
      public T getT() {
        throw new RuntimeException();
      }
    }
  }

  static class SubConfig<C extends MyClass<C>> extends Config<C> {
    public class SubConfigInner extends ConfigInner<Thing> {
      public C outerClassTypeVar() {
        throw new RuntimeException();
      }
    }

    class Thing {
      public SubConfigInner getSubConfigInner() {
        throw new RuntimeException();
      }
    }
  }
}
