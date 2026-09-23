/*
 * @test
 * @summary Test -AwarnUnneededSuppressions
 *
 * @compile/ref=UnneededSuppressionsNullnessTest.goal -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -AwarnUnneededSuppressions UnneededSuppressionsNullnessTest.java
 */

class UnneededSuppressionsNullnessTest {

  @SuppressWarnings({"nullness:return"})
  public String getClassAndUid1() {
    return "hello";
  }

  @SuppressWarnings({"nullness:return", "unneeded.suppression"})
  public String getClassAndUid2() {
    return "hello";
  }

  @SuppressWarnings({"nullness:return", "nullness:unneeded.suppression"})
  public String getClassAndUid3() {
    return "hello";
  }

  @SuppressWarnings({"unneeded.suppression", "nullness:return"})
  public String getClassAndUid5() {
    return "hello";
  }

  @SuppressWarnings({"nullness:unneeded.suppression", "nullness:return"})
  public String getClassAndUid6() {
    return "hello";
  }

  @SuppressWarnings({"nullness", "unneeded.suppression"})
  public String getClassAndUid7() {
    return "hello";
  }

  @SuppressWarnings({"nullness", "nullness:unneeded.suppression"})
  public String getClassAndUid8() {
    return "hello";
  }

  // A partial message key does not suppress the "unneeded.suppression" warning; only the
  // complete message key does.

  @SuppressWarnings({"nullness", "suppression"})
  public String getClassAndUid9() {
    return "hello";
  }

  @SuppressWarnings({"nullness", "unneeded"})
  public String getClassAndUid10() {
    return "hello";
  }

  @SuppressWarnings({"nullness", "nullness:suppression"})
  public String getClassAndUid11() {
    return "hello";
  }
}
