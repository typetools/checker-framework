/*
 * @test
 * @summary Test wildcards in stub files.
 *
 * @compile -XDrawDiagnostics -Xlint:unchecked NullableBox.java
 * @compile -XDrawDiagnostics -Xlint:unchecked -processor org.checkerframework.checker.nullness.NullnessChecker -Anomsgtext -Astubs=nullablebox-none.astub StubWildcardsNbl.java
 * @compile -XDrawDiagnostics -Xlint:unchecked -processor org.checkerframework.checker.nullness.NullnessChecker -Anomsgtext -Astubs=nullablebox-nullable.astub StubWildcardsNbl.java
 * @compile/fail/ref=StubWildcardsNbl.out -XDrawDiagnostics -Xlint:unchecked -processor org.checkerframework.checker.nullness.NullnessChecker -Anomsgtext -Astubs=nullablebox-object.astub -Werror StubWildcardsNbl.java
 * @compile/fail/ref=StubWildcardsNbl.out -XDrawDiagnostics -Xlint:unchecked -processor org.checkerframework.checker.nullness.NullnessChecker -Anomsgtext -Astubs=nullablebox-nonnull.astub -Werror StubWildcardsNbl.java
 */
import org.checkerframework.checker.nullness.qual.Nullable;

public class StubWildcardsNbl {
    void use(NullableBox<@Nullable String> bs) {
        NullableBox.consume(bs);
    }
}
