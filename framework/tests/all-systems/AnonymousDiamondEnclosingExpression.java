// Type-argument inference for an anonymous class with an explicit enclosing expression.

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public class AnonymousDiamondEnclosingExpression {
  class In<T> {
    In(T t) {}
  }

  class WithVarargsConstructor<T> {
    @SafeVarargs
    WithVarargsConstructor(T... ts) {}
  }

  class Two<T> {
    Two(String s, T t) {}
  }

  class Fun<T> {
    Fun(Function<T, T> f) {}
  }

  class TwoFun<T> {
    TwoFun(String s, Function<T, T> f) {}
  }

  void test(AnonymousDiamondEnclosingExpression outer, String u) {
    In<String> a = this.new In<>(u) {};
    In<String> b = outer.new In<>(u) {};
    In<String> c = new In<>(u) {};
    WithVarargsConstructor<String> d = outer.new WithVarargsConstructor<>() {};
    WithVarargsConstructor<String> e = outer.new WithVarargsConstructor<>(u, u) {};
    Two<String> f = outer.new Two<>(u, u) {};
  }

  void polyArguments(AnonymousDiamondEnclosingExpression outer) {
    In<List<String>> a = outer.new In<>(new ArrayList<>()) {};
    In<List<String>> b = outer.new In<List<String>>(new ArrayList<>()) {};
    In<List<String>> c = outer.new In<>(Collections.emptyList()) {};
    In<List<String>> d = outer.new In<List<String>>(Collections.emptyList()) {};
    Two<List<String>> e = outer.new Two<>("", new ArrayList<>()) {};
    Two<List<String>> f = outer.new Two<List<String>>("", Collections.emptyList()) {};
  }

  void functionalArguments(AnonymousDiamondEnclosingExpression outer) {
    Fun<String> a = outer.new Fun<>(x -> x) {};
    Fun<String> b = outer.new Fun<>(String::trim) {};
    TwoFun<String> c = outer.new TwoFun<>("", x -> x) {};
  }
}
