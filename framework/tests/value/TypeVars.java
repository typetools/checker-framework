import org.checkerframework.common.value.qual.ArrayLen;
import org.checkerframework.common.value.qual.ArrayLenRange;
import org.checkerframework.common.value.qual.IntRange;
import org.checkerframework.common.value.qual.IntVal;

public class TypeVars<K, V> {
  <T extends @IntVal(3) Integer> void arrayDimension(T size) {
    int @ArrayLen(3) [] array = new int[size];
    // :: error: [assignment]
    int @ArrayLen(4) [] wrongLength = new int[size];
  }

  <T extends @IntRange(from = 2, to = 5) Integer, U extends T> void arrayDimensionRange(U size) {
    int @ArrayLenRange(from = 2, to = 5) [] array = new int[size];
    // :: error: [assignment]
    int @ArrayLen(3) [] wrongLength = new int[size];
  }

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
