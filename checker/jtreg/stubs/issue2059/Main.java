/*
 * @test
 * @summary Test case for Issue 2059 https://github.com/typetools/checker-framework/issues/2059
 *
 * @compile/ref=AnnoNotFound.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -AstubWarnIfNotFound -Astubs=AnnoNotFound.astub -Anomsgtext Main.java
 */

package issue2059;

public class Main {}
