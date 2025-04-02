// @below-java21-jdk-skip-test

// None of the WPI formats supports the new Java 21 languages features, so skip inference until they
// do.
// @infer-jaifs-skip-test
// @infer-ajava-skip-test
// @infer-stubs-skip-test

public class FlowSwitch {

  void test0(Number n) {
    String s = null;
    switch (n) {
      case null, default:
        {
          // TODO: this should issue a dereference of nullable error.
          n.toString();
          s = "";
        }
    }
    s.toString();
  }

  void test1(Integer i) {
    String msg = null;
    switch (i) {
      case -1, 1:
        msg = "-1 or 1";
        break;
      case Integer j
      when j > 0:
        msg = "pos";
        break;
      case Integer j:
        msg = "everything else";
        break;
    }
    msg.toString();
  }

  void test2(Integer i) {
    String msg = null;
    switch (i) {
      case -1, 1:
        msg = "-1 or 1";
        break;
      default:
        msg = "everythingything else";
        break;
      case 2:
        msg = "pos";
        break;
    }
    msg.toString();
  }

  class A {}

  class B extends A {}

  sealed interface I permits C, D {}

  final class C implements I {}

  final class D implements I {}

  record Pair<T>(T x, T y) {}

  void testE(Pair<A> p1) {
    B e =
        switch (p1) {
          case Pair<A>(A a, B b) -> b;
          case Pair<A>(B b, A a) -> b;
          default -> null;
        };
    B e2 = null;
    switch (p1) {
      case Pair<A>(A a, B b) -> e2 = b;
      case Pair<A>(B b, A a) -> e2 = b;
      default -> e2 = new B();
    }
    e2.toString();
  }

  void test3(Pair<I> p2) {
    String s = null;
    I e = null;
    switch (p2) {
      case Pair<I>(I i, C c) -> {
        e = c;
        s = "";
      }
      case Pair<I>(I i, D d) -> {
        e = d;
        s = "";
      }
    }
    s.toString();
    e.toString();

    I e2 = null;
    switch (p2) {
      case Pair<I>(C c, I i) -> e2 = c;
      case Pair<I>(D d, C c) -> e2 = d;
      case Pair<I>(D d1, D d2) -> e2 = d2;
    }
    e2.toString();
  }
}
