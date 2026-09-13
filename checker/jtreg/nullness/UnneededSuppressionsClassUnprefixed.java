/*
 * @test
 * @summary Test -AwarnUnneededSuppressions
 *
 * @compile/ref=UnneededSuppressionsClassUnprefixed.goal -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -AwarnUnneededSuppressions UnneededSuppressionsClassUnprefixed.java
 */

@SuppressWarnings("unneeded.suppression")
class UnneededSuppressionsClassAnnotated {

  @SuppressWarnings("nullness:return")
  public String getClassAndUid0() {
    return "hello";
  }
}
