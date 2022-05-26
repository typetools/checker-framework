import org.checkerframework.checker.nullness.qual.Nullable;

/*
 * @test
 * @summary Test class type parameters in stub files.
 *
 * @compile -XDrawDiagnostics -Xlint:unchecked NullableBox.java
 * @compile -XDrawDiagnostics -Xlint:unchecked -processor org.checkerframework.checker.nullness.NullnessChecker -Anomsgtext -Astubs=nullablebox-none.astub StubTypeParamsClassNbl.java
 * @compile -XDrawDiagnostics -Xlint:unchecked -processor org.checkerframework.checker.nullness.NullnessChecker -Anomsgtext -Astubs=nullablebox-nullable.astub StubTypeParamsClassNbl.java
 * @compile/fail/ref=StubTypeParamsClassNbl.out -XDrawDiagnostics -Xlint:unchecked -processor org.checkerframework.checker.nullness.NullnessChecker -Anomsgtext -Astubs=nullablebox-object.astub -Werror StubTypeParamsClassNbl.java
 * @compile/fail/ref=StubTypeParamsClassNbl.out -XDrawDiagnostics -Xlint:unchecked -processor org.checkerframework.checker.nullness.NullnessChecker -Anomsgtext -Astubs=nullablebox-nonnull.astub -Werror StubTypeParamsClassNbl.java
 */
public class StubTypeParamsClassNbl {
    @Nullable NullableBox<@Nullable Object> f;
}
