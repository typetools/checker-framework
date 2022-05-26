import org.checkerframework.checker.nullness.qual.Nullable;

/*
 * @test
 * @summary Test class type parameters in stub files.
 *
 * @compile -XDrawDiagnostics -Xlint:unchecked Box.java
 * @compile -XDrawDiagnostics -Xlint:unchecked -processor org.checkerframework.checker.nullness.NullnessChecker -Anomsgtext -Astubs=box-none.astub StubTypeParamsClass.java
 * @compile -XDrawDiagnostics -Xlint:unchecked -processor org.checkerframework.checker.nullness.NullnessChecker -Anomsgtext -Astubs=box-nullable.astub StubTypeParamsClass.java
 * @compile/fail/ref=StubTypeParamsClass.out -XDrawDiagnostics -Xlint:unchecked -processor org.checkerframework.checker.nullness.NullnessChecker -Anomsgtext -Astubs=box-object.astub -Werror StubTypeParamsClass.java
 * @compile/fail/ref=StubTypeParamsClass.out -XDrawDiagnostics -Xlint:unchecked -processor org.checkerframework.checker.nullness.NullnessChecker -Anomsgtext -Astubs=box-nonnull.astub -Werror StubTypeParamsClass.java
 */
public class StubTypeParamsClass {
    @Nullable Box<@Nullable Object> f;
}
