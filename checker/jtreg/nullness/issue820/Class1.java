/*
 * @test
 * @summary Test case for Issue 820 https://github.com/typetools/checker-framework/issues/820
 *
 * @compile/fail/ref=Class1Class2-err.out -XDrawDiagnostics -Xlint:unchecked -processor org.checkerframework.checker.nullness.NullnessChecker -Anomsgtext Class1.java Class2.java
 * @compile/fail/ref=Class2Class1-err.out -XDrawDiagnostics -Xlint:unchecked -processor org.checkerframework.checker.nullness.NullnessChecker -Anomsgtext Class2.java Class1.java
 *
 */

import org.checkerframework.checker.nullness.qual.EnsuresNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.RequiresNonNull;

public class Class1 {
  public static @Nullable Object field = null;
  public @Nullable Object instanceField = null;

  @EnsuresNonNull("#1.instanceField")
  public static void method3(Class2 class2) {
    class2.instanceField = new Object();
  }

  @EnsuresNonNull("#1.instanceField")
  public static void method4(Class2 class2) {
    class2.instanceField = new Object();
  }

  @EnsuresNonNull("#1.instanceField")
  public static void method5(Class2 class2) {}

  @EnsuresNonNull("#1")
  public static void method6(Class2 class2) {}

  @RequiresNonNull("#1.instanceField")
  public static void method3R(Class2 class2) {
    class2.instanceField.toString();
  }
}
