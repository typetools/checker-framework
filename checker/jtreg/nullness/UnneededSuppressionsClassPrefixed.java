/*
 * @test
 * @summary Test -AwarnUnneededSuppressions
 *
 * @compile/ref=UnneededSuppressionsClassPrefixed.goal -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -AwarnUnneededSuppressions UnneededSuppressionsClassPrefixed.java
 */

@SuppressWarnings("nullness:unneeded.suppression")
class UnneededSuppressionsClassAnnotated {

  @SuppressWarnings("nullness:return")
  public String getClassAndUid0() {
    return "hello";
  }
}
