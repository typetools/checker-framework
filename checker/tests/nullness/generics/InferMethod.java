// From issue #216:
// https://github.com/typetools/checker-framework/issues/216

public class InferMethod {
  public abstract static class Generic<T> {
    public class Nested {
      public void nestedMethod(T item) {}
    }

    public static class NestedStatic<TInner> {
      public void nestedMethod2(TInner item) {}
    }

    public abstract void method();

    public abstract void method2();

    public void method3(T item) {}
  }

  public static class Concrete extends Generic<String> {

    @Override
    public void method() {
      Nested o = new Nested();
      o.nestedMethod("test");
    }

    @Override
    public void method2() {
      NestedStatic<String> o = new NestedStatic<>();
      o.nestedMethod2("test");

      this.method3("test");
    }
  }
}
