/*
 * @test
 * @summary Test for Issue 141
 *
 * @compile -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker Appendable.java Driver.java InputSupplier.java Readable.java CharStreams.java Files.java OutputStreamWriter.java Closeable.java InputStreamReader.java OutputSupplier.java
 * @compile -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker Files.java
 *
 * @compile -XDrawDiagnostics -processor org.checkerframework.checker.regex.RegexChecker Appendable.java Driver.java InputSupplier.java Readable.java CharStreams.java Files.java OutputStreamWriter.java Closeable.java InputStreamReader.java OutputSupplier.java
 * @compile -XDrawDiagnostics -processor org.checkerframework.checker.regex.RegexChecker Files.java
 *
 */
public class Driver {}
