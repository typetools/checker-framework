package sideeffectsonly;

import org.checkerframework.checker.tainting.qual.Tainted;
import org.checkerframework.checker.tainting.qual.Untainted;
import org.checkerframework.dataflow.qual.SideEffectFree;

public class EmptySideEffectsOnly2 {
  @Tainted Object x;
  @Untainted Object untainted;

  void test() {
    x = untainted;
    method1(x);
    // There should be no error here, because method1 has no side effect.
    method2(x);
  }

  @SideEffectFree
  void method1(@Untainted Object x) {}

  void method2(@Untainted Object x) {}
}
