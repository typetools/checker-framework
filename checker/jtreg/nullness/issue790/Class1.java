/*
 * @test
 * @summary Test for Issue #790
 *
 * @compile/fail/ref=expected.out -XDrawDiagnostics -Xlint:unchecked -processor org.checkerframework.checker.nullness.NullnessChecker -Anomsgtext Class1.java Class2.java
 *
 * @compile/fail/ref=expected.out -XDrawDiagnostics -Xlint:unchecked -processor org.checkerframework.checker.nullness.NullnessChecker -Anomsgtext Class2.java Class1.java
 *
 */
public class Class1 {
    Class1() {
        super(this);
    }
}
