// A lambda or a method reference whose target is an intersection type that has a bound with a
// wildcard type argument.  javac uses the non-wildcard parameterization of the bound (JLS 9.9).

import java.io.Serializable;
import java.util.Comparator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class IntersectionCastWildcard {

  interface NumberSupplier<T extends Number> {
    T get();
  }

  static String m() {
    return "";
  }

  static void take(Object o) {}

  // Generic, so that a method reference to it is compatible with a raw functional interface, whose
  // parameter type has the top qualifier in every type system.
  static <T> void takeAny(T t) {}

  static <T> T identity(T t) {
    return t;
  }

  void methodReferences() {
    Object o1 = (Serializable & Supplier<? extends String>) IntersectionCastWildcard::m;
    Object o2 = (Supplier<? extends CharSequence> & Serializable) IntersectionCastWildcard::m;
    Object o3 = (Serializable & Consumer<? super String>) IntersectionCastWildcard::take;
    Object o4 = (Serializable & Function<? super String, ? extends Object>) String::length;
    Object o5 = (Serializable & Supplier<? extends String>) (IntersectionCastWildcard::m);
  }

  void lambdas() {
    Object o1 = (Serializable & Supplier<? extends String>) () -> "";
    Object o2 = (Serializable & Consumer<? super String>) s -> s.length();
    Object o3 = (Serializable & Function<? super String, ? extends Object>) s -> s.length();
    Object o4 = (Serializable & Supplier<? extends String>) (() -> "");
  }

  // An unbounded wildcard, whose ground type argument is the bound of the type parameter.
  void unboundedWildcards() {
    Object o1 = (Serializable & Comparator<?>) (a, b) -> 0;
    Object o2 = (Serializable & NumberSupplier<?>) () -> 1;
    Object o3 = (Serializable & NumberSupplier<? extends Integer>) () -> 1;
  }

  // A raw functional interface, whose function type is erased (JLS 9.9).
  @SuppressWarnings("rawtypes")
  void rawTypes() {
    Object o1 = (Serializable & Supplier) IntersectionCastWildcard::m;
    Object o2 = (Serializable & Consumer) IntersectionCastWildcard::takeAny;
    Object o3 = (Serializable & Function) IntersectionCastWildcard::identity;
    Object o4 = (Serializable & Supplier) () -> "";
    Object o5 = (Serializable & Consumer) s -> {};
    Object o6 = (Serializable & Function) s -> s;
  }
}
