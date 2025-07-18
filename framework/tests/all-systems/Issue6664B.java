import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

public abstract class Issue6664B {

  @SuppressWarnings("purity.methodref")
  void method2() {
    // Doesn't use variable arity for method applicability.
    Function<Object[], List<Object>> f1 = Arrays::asList;

    // Equivalent to Arrays.asList(obj).
    Function<Object, List<Object>> f2 = Arrays::asList;
    // Equivalent to Arrays.asList(o1,o2,o3).
    Foo f3 = Arrays::asList;
    // Equivalent to Arrays.asList().
    Foo2 f4 = Arrays::asList;
  }

  interface Foo {

    List<Object> apply(Object o1, Object o2, Object o3);
  }

  interface Foo2 {

    List<Object> apply();
  }
}
