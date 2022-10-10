/*
 * @test
 * @summary Test -AwarnUnneededSuppressions
 *
 * @compile/ref=UnneededSuppressionsTest.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -AwarnUnneededSuppressions UnneededSuppressionsTest.java
 */

class UnneededSuppressionsTest {

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
}
