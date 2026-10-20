// Type-argument inference for an anonymous class with an explicit enclosing expression.

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AnonymousDiamondEnclosingExpression {
  class In<T> {
    In(T t) {}
  }

  class InVarargs<T> {
    @SafeVarargs
    InVarargs(T... ts) {}
  }

  class Two<T> {
    Two(String s, T t) {}
  }

  void test(AnonymousDiamondEnclosingExpression outer, String u) {
    In<String> a = this.new In<>(u) {};
    In<String> b = outer.new In<>(u) {};
    In<String> c = new In<>(u) {};
    InVarargs<String> d = outer.new InVarargs<>() {};
    InVarargs<String> e = outer.new InVarargs<>(u, u) {};
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
}
