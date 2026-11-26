// A lambda or a method reference whose target is an intersection type, where the functional method
// is declared by a bound other than the first bound that is a functional interface.  The functional
// method is Narrow.get, whose return type is more specific than that of Wide.get.

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
