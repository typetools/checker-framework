/*
 * @test
 * @summary Test case for issue #2173: https://github.com/typetools/checker-framework/issues/2173
 * See README in this directory.
 *
 * @compile -processor org.checkerframework.checker.nullness.NullnessChecker View.java
 */

public class View {
    private static void createTable() {
        ImporterManager.chooseAndImportFile("");
    }
}
