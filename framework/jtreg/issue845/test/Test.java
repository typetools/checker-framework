/*
 * @test
 * @summary Testing that a checker isn't required to be in a package. (Issue 845, https://github.com/typetools/checker-framework/issues/845)  The checker java files have to be in a separate directory otherwise $CHECKERFRAMEWORK/framework/jtreg/checker/qual directory is on the classpath before the qual directory with the compiled classes.
 *
 * @compile -XDrawDiagnostics -Xlint:unchecked ../checker/NotInPackageChecker.java ../checker/qual/NotInPackageTop.java
 * @compile -XDrawDiagnostics -Xlint:unchecked -processor NotInPackageChecker Test.java -proc:only
 */
public class Test {}
