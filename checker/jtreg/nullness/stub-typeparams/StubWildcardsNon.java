/*
 * @test
 * @summary Test wildcards in stub files.
 *
 * @compile -XDrawDiagnostics -Xlint:unchecked NullableBox.java
 * @compile -XDrawDiagnostics -Xlint:unchecked -processor org.checkerframework.checker.nullness.NullnessChecker -Anomsgtext -Astubs=nullablebox-none.astub StubWildcardsNon.java
 * @compile -XDrawDiagnostics -Xlint:unchecked -processor org.checkerframework.checker.nullness.NullnessChecker -Anomsgtext -Astubs=nullablebox-nullable.astub StubWildcardsNon.java
 * @compile/fail/ref=StubWildcardsNon.out -XDrawDiagnostics -Xlint:unchecked -processor org.checkerframework.checker.nullness.NullnessChecker -Anomsgtext -Astubs=nullablebox-object.astub -Werror StubWildcardsNon.java
 * @compile/fail/ref=StubWildcardsNon.out -XDrawDiagnostics -Xlint:unchecked -processor org.checkerframework.checker.nullness.NullnessChecker -Anomsgtext -Astubs=nullablebox-nonnull.astub -Werror StubWildcardsNon.java
 */
import org.checkerframework.checker.nullness.qual.Nullable;

public class StubWildcardsNon {
    void use(NullableBox<@Nullable String> bs) {
        NullableBox.nonnull(bs);
    }
}
