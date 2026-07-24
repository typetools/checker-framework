import org.checkerframework.checker.tainting.qual.Tainted;
import org.checkerframework.checker.tainting.qual.Untainted;
import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.dataflow.qual.SideEffectsOnly;
import org.checkerframework.framework.qual.EnsuresQualifier;

public class SideEffectsOnlyField {
  @Tainted Object a;
  @Tainted Object b;

  static void test(SideEffectsOnlyField arg) {
    method(arg);
    method3(arg);
    // :: error: argument
    method2(arg.a);
    method2(arg.b);
  }

  @EnsuresQualifier(
      expression = {"#1.a", "#1.b"},
      qualifier = Untainted.class)
  // :: error: contracts.postcondition
  static void method(SideEffectsOnlyField x) {}

  @SideEffectsOnly("#1.a")
  static void method3(SideEffectsOnlyField z) {}

  @SideEffectFree
  static void method2(@Untainted Object x) {}
}
