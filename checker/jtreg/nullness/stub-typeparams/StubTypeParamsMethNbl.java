/*
 * @test
 * @summary Test method type parameters in stub files.
 *
 * @compile -XDrawDiagnostics -Xlint:unchecked NullableBox.java
 * @compile -XDrawDiagnostics -Xlint:unchecked -processor org.checkerframework.checker.nullness.NullnessChecker -Anomsgtext -Astubs=nullablebox-none.astub StubTypeParamsMethNbl.java
 * @compile -XDrawDiagnostics -Xlint:unchecked -processor org.checkerframework.checker.nullness.NullnessChecker -Anomsgtext -Astubs=nullablebox-nullable.astub StubTypeParamsMethNbl.java
 * @compile/fail/ref=StubTypeParamsMethNbl.out -XDrawDiagnostics -Xlint:unchecked -processor org.checkerframework.checker.nullness.NullnessChecker -Anomsgtext -Astubs=nullablebox-object.astub -Werror StubTypeParamsMethNbl.java
 * @compile/fail/ref=StubTypeParamsMethNbl.out -XDrawDiagnostics -Xlint:unchecked -processor org.checkerframework.checker.nullness.NullnessChecker -Anomsgtext -Astubs=nullablebox-nonnull.astub -Werror StubTypeParamsMethNbl.java
 */
public class StubTypeParamsMethNbl {
    void use() {
        NullableBox.of(null);
    }
}
