/*
 * @test
 * @summary Test -AwarnUnneededSuppressions
 *
 * @compile/ref=UnneededSuppressionsClassUnprefixed.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -AwarnUnneededSuppressions UnneededSuppressionsClassUnprefixed.java
 */

@SuppressWarnings("unneeded.suppression")
class UnneededSuppressionsClassAnnotated {

    @SuppressWarnings("nullness:return.type.incompatible")
    public String getClassAndUid0() {
        return "hello";
    }
}
