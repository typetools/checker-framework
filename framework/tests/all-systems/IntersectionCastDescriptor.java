// A lambda or a method reference whose target is an intersection type, in which the functional
// interface method is not declared by the first bound that is a functional interface.  javac
// chooses `Narrow.get`, whose return type is more specific than that of `Wide.get`, as the
// functional interface method.

import java.io.Serializable;

public class IntersectionCastDescriptor {

  interface Wide {
    Object get();
  }

  interface Narrow<T> {
    T get();
  }

  static String s() {
    return "ok";
  }

  void lambdas() {
    Object o1 = (Wide & Narrow<String>) () -> "ok";
    Object o2 = (Narrow<String> & Wide) () -> "ok";
    Object o3 = (Serializable & Wide & Narrow<String>) () -> "ok";
  }

  void methodReferences() {
    Object o1 = (Wide & Narrow<String>) IntersectionCastDescriptor::s;
    Object o2 = (Narrow<String> & Wide) IntersectionCastDescriptor::s;
    Object o3 = (Serializable & Wide & Narrow<String>) IntersectionCastDescriptor::s;
  }
}
