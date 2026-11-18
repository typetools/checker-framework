// Test case for https://github.com/typetools/checker-framework/issues/8017

import org.checkerframework.checker.tainting.qual.Tainted;
import org.checkerframework.checker.tainting.qual.Untainted;

public class TaintingIssue8017 {
  class Inner {
    Inner(@Untainted TaintingIssue8017 o) {}

    Inner(TaintingIssue8017 o, @Untainted String s) {}
  }

  class InnerVarargs {
    InnerVarargs(TaintingIssue8017 o, @Untainted String... s) {}
  }

  void test(
      TaintingIssue8017 outer,
      @Untainted TaintingIssue8017 untaintedOuter,
      @Tainted TaintingIssue8017 taintedOuter,
      @Untainted String untainted,
      @Tainted String tainted) {
    outer.new Inner(untaintedOuter) {};
    // :: error: (argument)
    outer.new Inner(taintedOuter) {};
    this.new Inner(untaintedOuter) {};
    // :: error: (argument)
    this.new Inner(taintedOuter) {};
    outer.new Inner(outer, untainted) {};
    // :: error: (argument)
    outer.new Inner(outer, tainted) {};
    new Inner(untaintedOuter) {};
    // :: error: (argument)
    new Inner(taintedOuter) {};
    outer.new InnerVarargs(outer, untainted, untainted) {};
    // :: error: (argument)
    outer.new InnerVarargs(outer, untainted, tainted) {};
  }
}
