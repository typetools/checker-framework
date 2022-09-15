/*
 * @test
 * @summary Test -AwarnUnneededSuppressions
 *
 * @compile/ref=UnneededSuppressionsTest.out -XDrawDiagnostics -processor org.checkerframework.checker.index.IndexChecker,org.checkerframework.checker.nullness.NullnessChecker -AwarnUnneededSuppressions UnneededSuppressionsTest.java
 */

@SuppressWarnings("nullness:unneeded.suppression")
/*
@SuppressWarnings("index")
*/
public class UnneededSuppressionsTest {

  /*
  void method(@NonNegative int i) {
    @SuppressWarnings("index")
    @NonNegative int x = i - 1;
  }

  void method2() {
    @SuppressWarnings("fallthrough")
    int x = 0;
  }

  @SuppressWarnings({"tainting", "lowerbound"})
  void method3() {
    @SuppressWarnings("upperbound:assignment")
    int z = 0;
  }

  void method4() {
    @SuppressWarnings("lowerbound:assignment")
    @NonNegative int x = -1;
  }

  @SuppressWarnings("purity.not.deterministic.call")
  void method5() {}

  @SuppressWarnings("purity")
  void method6() {}

  @SuppressWarnings("index:foo.bar.baz")
  void method7() {}

  @SuppressWarnings("allcheckers:purity.not.deterministic.call")
  void method8() {}

  @SuppressWarnings("nullness:return")
  public String getClassAndUid0() {
    return "hello";
  }
  */

  @SuppressWarnings({"nullness:return"})
  public String getClassAndUid1() {
    return "hello";
  }

  /*
  @SuppressWarnings({"nullness:return", "unneeded.suppression"})
  public String getClassAndUid2() {
    return "hello";
  }

  @SuppressWarnings({"nullness:return", "nullness:unneeded.suppression"})
  public String getClassAndUid3() {
    return "hello";
  }
  */

  /*
  @SuppressWarnings({"unneeded.suppression", "nullness:return"})
  public String getClassAndUid5() {
    return "hello";
  }

  @SuppressWarnings({"nullness:unneeded.suppression", "nullness:return"})
  public String getClassAndUid6() {
    return "hello";
  }
  */
}
