/*
 * @test
 * @summary
 * Test case for Issue 1051
 * https://github.com/typetools/checker-framework/issues/1051
 *
 * @compile/fail/ref=Class1.out -XDrawDiagnostics -Xlint:unchecked -processor org.checkerframework.checker.nullness.NullnessChecker -Anomsgtext Class1.java
 *
 */

import org.checkerframework.checker.nullness.qual.RequiresNonNull;

public class Class1 {
    @RequiresNonNull("instanceField")
    public static void foo() {}
}
