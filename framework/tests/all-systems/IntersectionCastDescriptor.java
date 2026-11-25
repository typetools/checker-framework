// A lambda or a method reference whose target is an intersection type, in which the functional
// interface method is not declared by the first bound that is a functional interface.  javac
// chooses `Generic.get`, whose return type is the most specific, as the functional interface
// method.

public class IntersectionCastDescriptor {

  interface Plain {
    Object get();
  }

  interface Generic<T> {
    T get();
  }

  static String m() {
    return "";
  }

  void lambdas() {
    Object o1 = (Plain & Generic<String>) () -> "";
    Object o2 = (Generic<String> & Plain) () -> "";
  }

  void methodReferences() {
    Object o1 = (Plain & Generic<String>) IntersectionCastDescriptor::m;
    Object o2 = (Generic<String> & Plain) IntersectionCastDescriptor::m;
  }
}
