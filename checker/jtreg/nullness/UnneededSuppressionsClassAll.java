/*
 * @test
 * @summary Test -AwarnUnneededSuppressions
 *
 * @compile/ref=UnneededSuppressionsClassAll.goal -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -AwarnUnneededSuppressions UnneededSuppressionsClassAll.java
 */

// The "all" in the class annotation does not suppress the "unneeded.suppression" warning about
// the method annotation, but the "unneeded.suppression" in the class annotation does.
@SuppressWarnings({"all", "unneeded.suppression"})
class UnneededSuppressionsClassAllAnnotated {

  @SuppressWarnings("nullness:return")
  public String getClassAndUid0() {
    return "hello";
  }
}

// Without "unneeded.suppression", the warning about the method annotation is issued.
@SuppressWarnings("all")
class UnneededSuppressionsClassAllControl {

  @SuppressWarnings("nullness:return")
  public String getClassAndUid0() {
    return "hello";
  }
}
