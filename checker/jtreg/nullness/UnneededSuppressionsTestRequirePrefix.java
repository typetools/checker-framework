/*
 * @test
 * @summary Test -AwarnUnneededSuppressions
 *
 * @compile/ref=UnneededSuppressionsTestRequirePrefix.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -AwarnUnneededSuppressions -ArequirePrefixInWarningSuppressions UnneededSuppressionsTestRequirePrefix.java
 */

class UnneededSuppressionsTest {

    @SuppressWarnings({"nullness:return.type.incompatible"})
    public String getClassAndUid1() {
        return "hello";
    }

    @SuppressWarnings({"nullness:return.type.incompatible", "unneeded.suppression"})
    public String getClassAndUid2() {
        return "hello";
    }

    @SuppressWarnings({"nullness:return.type.incompatible", "nullness:unneeded.suppression"})
    public String getClassAndUid3() {
        return "hello";
    }

    @SuppressWarnings({
        "unneeded.suppression.type.incompatible",
        "nullness:return.type.incompatible"
    })
    public String getClassAndUid5() {
        return "hello";
    }

    @SuppressWarnings({"nullness:unneeded.suppression", "nullness:return.type.incompatible"})
    public String getClassAndUid6() {
        return "hello";
    }
}
