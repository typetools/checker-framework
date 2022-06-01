/*
 * @test
 * @summary Test wildcards in stub files.
 *
 * @compile -XDrawDiagnostics -Xlint:unchecked Box.java
 * @compile -XDrawDiagnostics -Xlint:unchecked -processor org.checkerframework.checker.nullness.NullnessChecker -Anomsgtext -Astubs=box-none.astub StubWildcards.java
 * @compile -XDrawDiagnostics -Xlint:unchecked -processor org.checkerframework.checker.nullness.NullnessChecker -Anomsgtext -Astubs=box-nullable.astub StubWildcards.java
 * @compile/fail/ref=StubWildcards.out -XDrawDiagnostics -Xlint:unchecked -processor org.checkerframework.checker.nullness.NullnessChecker -Anomsgtext -Astubs=box-object.astub -Werror StubWildcards.java
 * @compile/fail/ref=StubWildcards.out -XDrawDiagnostics -Xlint:unchecked -processor org.checkerframework.checker.nullness.NullnessChecker -Anomsgtext -Astubs=box-nonnull.astub -Werror StubWildcards.java
 */
import org.checkerframework.checker.nullness.qual.Nullable;

public class StubWildcards {
    void use(Box<@Nullable String> bs) {
        Box.consume(bs);
    }
}
