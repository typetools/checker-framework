// @below-java21-jdk-skip-test

// @infer-jaifs-skip-test
// @infer-ajava-skip-test
// @infer-stubs-skip-test

// Tests for record patterns whose matched type mentions captured type variables, such as a record
// pattern nested in another one, whose record component has the type Box<CAP#1>.

import org.checkerframework.checker.tainting.qual.Untainted;

public class CapturedTypeArguments {

  record Box<T>(T t) {}

  record Outer<U>(Box<U> b, U u) {}

  record UntaintedOuter<U>(Box<@Untainted U> b) {}

  record Pair<A, B extends Box<A>>(A a, B b) {}

  void nestedExtends(Outer<? extends @Untainted CharSequence> o) {
    if (o instanceof Outer(Box(var s), var u)) {
      @Untainted CharSequence x = s;
      @Untainted CharSequence y = u;
    }
  }

  void nestedSuper(Outer<? super @Untainted String> o) {
    if (o instanceof Outer(Box(var s), var u)) {
      // :: error: (assignment)
      @Untainted Object x = s;
      // :: error: (assignment)
      @Untainted Object y = u;
    }
  }

  static <X> Box<X> self(Box<X> b) {
    return b;
  }

  void invocationResult(Box<? extends @Untainted CharSequence> b) {
    if (self(b) instanceof Box(var t)) {
      @Untainted CharSequence x = t;
    }
  }

  // The record component's type is Box<@Untainted CAP#1>.
  void qualifiedCapture(UntaintedOuter<?> o) {
    if (o instanceof UntaintedOuter(Box(var t))) {
      @Untainted Object x = t;
    }
    if (o.b() instanceof Box(var t)) {
      @Untainted Object x = t;
    }
  }

  void qualifiedCaptureTypePattern(UntaintedOuter<? extends String> o) {
    if (o instanceof UntaintedOuter(Box(String t))) {
      @Untainted String x = t;
    }
  }

  // The type of b is CAP#2 extends Box<CAP#1>.
  void captureInBound(Pair<? extends @Untainted String, ?> p) {
    if (p instanceof Pair(var a, Box(var t))) {
      @Untainted String x = a;
      @Untainted String y = t;
    }
    if (p instanceof Pair(var a, Box(String t))) {
      @Untainted String y = t;
    }
    switch (p) {
      case Pair(var a, Box(var t)) -> {
        @Untainted String y = t;
      }
    }
  }

  void captureInBoundTainted(Pair<? extends String, ?> p) {
    if (p instanceof Pair(var a, Box(var t))) {
      // :: error: (assignment)
      @Untainted String y = t;
    }
  }

  static <X extends Box<? extends @Untainted String>> void typeVariable(X x) {
    if (x instanceof Box(var t)) {
      @Untainted String y = t;
    }
  }
}
