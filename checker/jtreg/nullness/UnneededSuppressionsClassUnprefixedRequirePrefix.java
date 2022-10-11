/*
 * @test
 * @summary Test -AwarnUnneededSuppressions
 *
 * @compile/ref=UnneededSuppressionsClassUnprefixedRequirePrefix.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -AwarnUnneededSuppressions -ArequirePrefixInWarningSuppressions UnneededSuppressionsClassUnprefixedRequirePrefix.java
 */

@SuppressWarnings("unneeded.suppression")
class UnneededSuppressionsClassAnnotated {

    @SuppressWarnings("nullness:return.type.incompatible")
    public String getClassAndUid0() {
        return "hello";
    }
}
