// A lambda or a method reference whose target is an intersection type that has a bound with a
// wildcard type argument.  javac uses the non-wildcard parameterization of the bound (JLS 9.9).

import java.io.Serializable;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class IntersectionCastWildcard {

  static String m() {
    return "";
  }

  static void take(Object o) {}

  void methodReferences() {
    Object o1 = (Serializable & Supplier<? extends String>) IntersectionCastWildcard::m;
    Object o2 = (Supplier<? extends CharSequence> & Serializable) IntersectionCastWildcard::m;
    Object o3 = (Serializable & Consumer<? super String>) IntersectionCastWildcard::take;
    Object o4 = (Serializable & Function<? super String, ? extends Object>) String::length;
  }

  void lambdas() {
    Object o1 = (Serializable & Supplier<? extends String>) () -> "";
    Object o2 = (Serializable & Consumer<? super String>) s -> s.length();
    Object o3 = (Serializable & Function<? super String, ? extends Object>) s -> s.length();
  }
}
