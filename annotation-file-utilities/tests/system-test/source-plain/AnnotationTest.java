package annotations.tests;

import java.util.*;

public class AnnotationTest<Foo extends Comparable<Integer>> {
  class Outer {
    class Inner<Baz> {
      int baz(Baz o) {
        return o.hashCode() ^ this.hashCode();
      }
    }
  }

  Iterable<String[]> field;
  Outer.Inner<String> inner;
  Map.Entry<Integer, ? extends CharSequence> entry;

  <Bar extends Comparable<Integer>> HashSet<Integer> doSomething(Set<Integer> param) {
    HashSet<Integer> local;
    if (param instanceof HashSet) local = (HashSet<Integer>) param;
    else local = new HashSet<Integer>();
    return local;
  }
}
