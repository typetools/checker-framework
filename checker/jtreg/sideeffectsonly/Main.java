/*
 * @test
 * @summary Test that a `@SideEffectsOnly` diagnostic is issued once per call site:  not once per
 * dataflow iteration, and not once per checker of a compound checker.  The Nullness Checker is a
 * compound checker that runs its own dataflow analysis, and the Internationalization Checker is an
 * aggregate checker that does not.
 *
 * @compile/fail/ref=SideEffectsOnlyDiagnostics.goal -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -AcheckPurityAnnotations SideEffectsOnlyDiagnostics.java
 * @compile/fail/ref=SideEffectsOnlyDiagnostics.goal -XDrawDiagnostics -processor org.checkerframework.checker.i18n.I18nChecker -AcheckPurityAnnotations SideEffectsOnlyDiagnostics.java
 */

public class Main {}
