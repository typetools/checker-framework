/*
 * @test
 * @summary Test case for issue #2173: https://github.com/typetools/checker-framework/issues/2173
 * See README in this directory.
 *
 * @compile/ref=View.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker View.java -Anomsgtext
 * @compile -processor org.checkerframework.checker.nullness.NullnessChecker View.java -AignoreInvalidAnnotationLocations -Werror
 */

public class View {
  private static void createTable() {
    ImporterManager.chooseAndImportFile("");
  }
}
