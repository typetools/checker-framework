/*
 * @test
 * @summary Test -AwarnUnneededSuppressions
 *
 * @compile/ref=UnneededSuppressionsClassPrefixedRequirePrefix.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -AwarnUnneededSuppressions -ArequirePrefixInWarningSuppressions UnneededSuppressionsClassPrefixedRequirePrefix.java
 */

@SuppressWarnings("nullness:unneeded.suppression")
class UnneededSuppressionsClassAnnotated {

    @SuppressWarnings("nullness:return")
    public String getClassAndUid0() {
        return "hello";
    }
}
