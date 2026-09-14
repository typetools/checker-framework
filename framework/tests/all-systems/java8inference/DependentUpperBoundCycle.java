// Test case for the last sentence of JLS 18.1.3: if the substituted upper bounds of a type
// parameter are all dependencies on other inference variables, then the bound
// "alpha <: Object" is also implied.

import java.util.List;
import java.util.Map;

public class DependentUpperBoundCycle {

  static <T extends S, S extends List<T>> T listCycle() {
    throw new AssertionError();
  }

  static <T extends S, S extends Comparable<T>> T comparableCycle() {
    throw new AssertionError();
  }

  static <K extends V, V extends Map<K, V>> K mapCycle() {
    throw new AssertionError();
  }

  static <T extends S, S extends Iterable<T>> List<T> listReturnCycle() {
    throw new AssertionError();
  }

  void use() {
    listCycle();
    comparableCycle();
    mapCycle();
    Object o = listCycle();
    List<?> l = listReturnCycle();
  }
}
