import java.util.Collection;

// Test case for https://github.com/typetools/checker-framework/issues/7217

class Issue7217<E extends Enum<E>> {

  void test(boolean b, Collection<E> c1, Collection<E> c2) {
    Object x = b ? c1 : c2;
  }

  boolean enumTypeVar(E elt, Enum<?> e) {
    e.ordinal();
    return elt == e;
  }

  boolean enumTypeVarReversed(E elt, Enum<?> e) {
    e.ordinal();
    return e == elt;
  }

  boolean enumTypeVarArray(E[] enumConstants, Enum<?> e) {
    int index = e.ordinal();
    return index < enumConstants.length && enumConstants[index] == e;
  }

  <T> boolean typeVar(T elt, Object o) {
    o.toString();
    return elt == o;
  }

  <T> boolean typeVarReversed(T elt, Object o) {
    o.toString();
    return o == elt;
  }
}
