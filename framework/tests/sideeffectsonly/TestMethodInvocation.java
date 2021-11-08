package sideeffectsonly;

import org.checkerframework.dataflow.qual.SideEffectsOnly;

public class TestMethodInvocation {
  @SideEffectsOnly("#1")
  void method1(Object o) {
    // :: error: purity.incorrect.sideeffectsonly
    method2();
    method3(o);
  }

  void method2() {}

  @SideEffectsOnly("#1")
  void method3(Object o) {}
}
