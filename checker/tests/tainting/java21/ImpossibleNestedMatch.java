// @below-java21-jdk-skip-test

// @infer-jaifs-skip-test
// @infer-ajava-skip-test
// @infer-stubs-skip-test

// javac checks a nested record pattern against the type of the record component in the Java type
// of the enclosing record pattern.  The inferred type of the enclosing record pattern can have a
// more specific component type, which no value of the nested record pattern's type can have.

import java.util.ArrayList;
import java.util.function.Supplier;

public class ImpossibleNestedMatch {

  record Outer<T>(T value) implements Supplier<T> {
    public T get() {
      return value;
    }
  }

  record Inner<U>(U value) {}

  // No ArrayList is an Inner.
  void unrelatedClass(Supplier<ArrayList<String>> s) {
    if (s instanceof Outer<?>(Inner(var x))) {}
  }

  record IntegerSupplier<E>(E e) implements Supplier<Integer> {
    public Integer get() {
      throw new Error();
    }
  }

  // No Supplier<String> is a Supplier<Integer>.
  void conflictingTypeArgument(Supplier<Supplier<String>> s) {
    if (s instanceof Outer<?>(IntegerSupplier(var x))) {}
  }
}
