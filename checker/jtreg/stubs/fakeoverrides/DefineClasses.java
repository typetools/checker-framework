package fakeoverrides;

import java.util.List;
import java.util.Map;

public class DefineClasses {}

interface SuperInterface {
  default int m() {
    return 0;
  }

  default int g(List<String> l) {
    return 0;
  }

  default int nested(Map.Entry<String, String> e) {
    return 0;
  }

  default int wildcard(List<? extends Number> l) {
    return 0;
  }

  default int varargs(String... s) {
    return 0;
  }

  default int enclosing(Enclosing<String>.Nested<Integer> e) {
    return 0;
  }

  default <T extends Number & Comparable<T>> int intersection(T t) {
    return 0;
  }
}

class Enclosing<T> {
  class Nested<U> {}
}

class SuperClass implements SuperInterface {
  // fake override:
  // @Untainted int m();
  // @Untainted int g(List<String> l);
  // @Untainted int nested(Map.Entry<String, String> e);
  // @Untainted int wildcard(List<? extends Number> l);
  // @Untainted int varargs(String... s);
  // @Untainted int enclosing(Enclosing<String>.Nested<Integer> e);
  // @Untainted int intersection(Number t);
}

interface SubInterface extends SuperInterface {
  // fake override:
  // int m();
}
