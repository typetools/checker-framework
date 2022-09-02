/*
 * @test
 * @summary Test for EISOP Issue #321
 * @compile/fail/ref=Issue321.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker Issue321.java -Anomsgtext
 * @compile -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -Astubs=Issue321.astub Issue321.java
 */

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Optional;

interface Issue321 {
    @Nullable Optional<String> o();
}
