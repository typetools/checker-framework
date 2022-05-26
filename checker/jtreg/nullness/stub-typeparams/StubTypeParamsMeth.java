/*
 * @test
 * @summary Test method type parameters in stub files.
 *
 * @compile -XDrawDiagnostics -Xlint:unchecked Box.java
 * @compile -XDrawDiagnostics -Xlint:unchecked -processor org.checkerframework.checker.nullness.NullnessChecker -Anomsgtext -Astubs=box-none.astub StubTypeParamsMeth.java
 * @compile -XDrawDiagnostics -Xlint:unchecked -processor org.checkerframework.checker.nullness.NullnessChecker -Anomsgtext -Astubs=box-nullable.astub StubTypeParamsMeth.java
 * @compile/fail/ref=StubTypeParamsMeth.out -XDrawDiagnostics -Xlint:unchecked -processor org.checkerframework.checker.nullness.NullnessChecker -Anomsgtext -Astubs=box-object.astub -Werror StubTypeParamsMeth.java
 * @compile/fail/ref=StubTypeParamsMeth.out -XDrawDiagnostics -Xlint:unchecked -processor org.checkerframework.checker.nullness.NullnessChecker -Anomsgtext -Astubs=box-nonnull.astub -Werror StubTypeParamsMeth.java
 */
public class StubTypeParamsMeth {
    void use() {
        Box.of(null);
    }
}
