// The functional interface type of a lambda or method reference argument to
// `outer.new Inner(...) {}` is the matching formal parameter, not the anonymous constructor's
// enclosing-instance parameter.

import java.util.function.Function;

public class AnonymousClassEnclosingExpressionLambda {
  class Inner {
    Inner(Runnable r) {}

    Inner(Object o, Function<String, String> f) {}
  }

  static void run() {}

  void m(AnonymousClassEnclosingExpressionLambda outer) {
    Object o1 = outer.new Inner(() -> {}) {};
    Object o2 = outer.new Inner("", s -> s) {};
    Object o3 = outer.new Inner(AnonymousClassEnclosingExpressionLambda::run) {};
  }
}
