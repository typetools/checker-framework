/*
 * @test
 * @summary Test variable name defaults.
 *
 * @clean lib.Test use.UseTest
 * @compile lib/Test.java
 * @compile -processor org.checkerframework.framework.testchecker.variablenamedefault.VariableNameDefaultChecker use/UseTest.java -XDrawDiagnostics
 *
 * @clean lib.Test use.UseTest
 * @compile -g lib/Test.java
 * @compile/fail/ref=UseTest1.out -processor org.checkerframework.framework.testchecker.variablenamedefault.VariableNameDefaultChecker use/UseTest.java -XDrawDiagnostics
 *
 * @clean lib.Test use.UseTest
 * @compile -processor org.checkerframework.framework.testchecker.variablenamedefault.VariableNameDefaultChecker lib/Test.java
 * @compile/fail/ref=UseTest2.out -processor org.checkerframework.framework.testchecker.variablenamedefault.VariableNameDefaultChecker use/UseTest.java -XDrawDiagnostics
 *
 * @clean lib.Test use.UseTest
 * @compile/fail/ref=UseTest1.out -processor org.checkerframework.framework.testchecker.variablenamedefault.VariableNameDefaultChecker use/UseTest.java lib/Test.java -XDrawDiagnostics
 */
class Driver {}
