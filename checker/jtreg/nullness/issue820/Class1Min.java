/*
 * @test
 * @summary Test case for Issue 820 https://github.com/typetools/checker-framework/issues/820
 *
 * @compile/fail/ref=Class1MinClass2Min-err.out -XDrawDiagnostics -Xlint:unchecked -processor org.checkerframework.checker.nullness.NullnessChecker -Anomsgtext Class1Min.java Class2Min.java
 * @compile/fail/ref=Class1MinClass2Min-err.out -XDrawDiagnostics -Xlint:unchecked -processor org.checkerframework.checker.nullness.NullnessChecker -Anomsgtext Class2Min.java Class1Min.java
 *
 */

import org.checkerframework.checker.nullness.qual.EnsuresNonNull;

public class Class1Min {
    @EnsuresNonNull("#1")
    public void methodInstance(Class2Min class2) {}
}
