// The functional interface type of a lambda or method reference argument to
// `outer.new Inner(...) {}` is the matching formal parameter, not the anonymous constructor's
// enclosing-instance parameter.

import java.util.function.Function;

public class AnonymousClassEnclosingExpressionLambda {
  class Inner {
    Inner(Runnable r) {}

    Inner(Object o, Function<String, String> f) {}
  }

  class VarargsInner {
    VarargsInner(Runnable... rs) {}
  }

  class Gen<T> {
    Gen(Function<T, T> f) {}
  }

  static void run() {}

  void m(AnonymousClassEnclosingExpressionLambda outer) {
    Object o1 = outer.new Inner(() -> {}) {};
    Object o2 = outer.new Inner("", s -> s) {};
    Object o3 = outer.new Inner(AnonymousClassEnclosingExpressionLambda::run) {};
    Object o4 = this.new Inner(() -> {}) {};
    Object o5 = new Inner(() -> {}) {};
    Object o6 = new Inner("", s -> s) {};
  }

  void varargs(AnonymousClassEnclosingExpressionLambda outer) {
    Object o1 = outer.new VarargsInner(() -> {}) {};
    Object o2 = outer.new VarargsInner(() -> {}, () -> {}) {};
    Object o3 = outer.new VarargsInner(AnonymousClassEnclosingExpressionLambda::run, () -> {}) {};
  }

  void generic(AnonymousClassEnclosingExpressionLambda outer) {
    Gen<String> g1 = outer.new Gen<String>(x -> x) {};
    Gen<String> g2 = outer.new Gen<String>(String::trim) {};
  }
}
