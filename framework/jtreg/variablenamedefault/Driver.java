/*
 * @test
 * @summary Test variable name defaults.
 *
 * @compile lib/Test.java
 * @compile/fail/ref=UseTest.out -processor org.checkerframework.framework.testchecker.variablenamedefault.VariableNameDefaultChecker use/UseTest.java -XDrawDiagnostics
 * @compile/fail/ref=UseTest.out -processor org.checkerframework.framework.testchecker.variablenamedefault.VariableNameDefaultChecker use/UseTest.java lib/Test.java -XDrawDiagnostics
 */
class Driver {}
