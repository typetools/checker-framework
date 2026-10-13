public class TypeVars<K, V> {
  private void test(K key, V value) {
    String s = "Negative size: " + key + "=" + value;
  }

  class MyClass<T> {
    public T myMethod() {
      return null;
    }
  }

  public class TypeVarDefaults {
    class ImplicitUpperBound<T> {}

    class ExplicitUpperBound<T extends Object> {}

    void useImplicit() {
      ImplicitUpperBound<Object> bottom;
    }

    void useExplicit() {
      ExplicitUpperBound<Object> bottom;
    }

    void wildCardImplicit() {
      ImplicitUpperBound<?> bottom;
    }

    void wildCardExplicit() {
      ExplicitUpperBound<?> bottom;
    }

    void wildCardUpperBoundImplicit() {
      ImplicitUpperBound<? extends Object> bottom;
    }

    void wildCardUpperBoundExplicit() {
      ExplicitUpperBound<? extends Object> bottom;
    }

    void wildCardLowerImplicit() {
      ImplicitUpperBound<? super Object> bottom;
    }

    void wildCardLowerBoundExplicit() {
      ExplicitUpperBound<? super Object> bottom;
    }
  }
}
