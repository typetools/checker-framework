package sideeffectsonly;

import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.dataflow.qual.SideEffectsOnly;
import org.checkerframework.framework.qual.EnsuresQualifier;
import org.checkerframework.framework.testchecker.sideeffectsonly.qual.SideEffectsOnlyToyBottom;

public class SideEffectsOnlyField {
  Object a;
  Object b;

  static void test(SideEffectsOnlyField arg) {
    method(arg);
    method3(arg);
    // :: error: argument.type.incompatible
    method2(arg.a);
    method2(arg.b);
  }

  @EnsuresQualifier(
      expression = {"#1.a", "#1.b"},
      qualifier = SideEffectsOnlyToyBottom.class)
  // :: error: contracts.postcondition.not.satisfied
  static void method(SideEffectsOnlyField x) {}

  @SideEffectsOnly("#1.a")
  static void method3(SideEffectsOnlyField z) {}

  @SideEffectFree
  static void method2(@SideEffectsOnlyToyBottom Object x) {}
}
