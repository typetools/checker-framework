package org.checkerframework.framework.test;

import java.io.File;
import java.util.List;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.signature.qual.BinaryName;

/** A configuration for running CheckerFrameworkTests or running the TypecheckExecutor. */
public interface TestConfiguration {
    /**
     * Returns a list of source files a CheckerFrameworkPerDirectoryTest should be run over. These
     * source files will be passed to Javac when the test is run. These are NOT JUnit tests.
     *
     * @return a list of source files a CheckerFrameworkPerDirectoryTest should be run over
     */
    List<File> getTestSourceFiles();

    /**
     * Diagnostic files consist of a set of lines that enumerate expected error/warning diagnostics.
     * The lines are of the form:
     *
     * <pre>fileName:lineNumber: diagnostKind: (messageKey)</pre>
     *
     * e.g.,
     *
     * <pre>MethodInvocation.java:17: error: (method.invocation.invalid)</pre>
     *
     * If getDiagnosticFiles does NOT return an empty list, then the only diagnostics expected by
     * the TestExecutor will be the ones found in these files. If it does return an empty list, then
     * the only diagnostics expected will be the ones found in comments in the input test files.
     *
     * <p>It is preferred that users write the errors in the test files and not in diagnostic files.
     *
     * @return a List of diagnostic files containing the error/warning messages expected to be
     *     output when Javac is run on the files returned by getTestSourceFiles. Return an empty
     *     list if these messages were specified within the source files.
     */
    List<File> getDiagnosticFiles();

    /**
     * Returns a list of annotation processors (Checkers) passed to the Javac compiler.
     *
     * @return a list of annotation processors (Checkers) passed to the Javac compiler
     */
    List<@BinaryName String> getProcessors();

    /**
     * Some Javac command line arguments require arguments themselves (e.g. {@code -classpath} takes
     * a path) getOptions returns a {@code Map(optionName => optionArgumentIfAny)}. If an option
     * does not take an argument, pass null as the value.
     *
     * <p>E.g.,
     *
     * <pre>{@code
     * Map(
     *   "-AprintAllQualifiers" => null
     *    "-classpath" => "myDir1:myDir2"
     * )
     * }</pre>
     *
     * @return a Map representing all command-line options to Javac other than source files and
     *     processors
     */
    Map<String, @Nullable String> getOptions();

    /**
     * Returns the map returned by {@link #getOptions}, flattened into a list. The entries will be
     * added as followed: List(key1, value1, key2, value2, ..., keyN, valueN). If a value is NULL,
     * then it will not appear in the list.
     *
     * @return the map returned {@link #getOptions}, but flattened into a list
     */
    List<String> getFlatOptions();

    /**
     * Returns true if the TypecheckExecutor should emit debug information on system out, false
     * otherwise.
     *
     * @return true if the TypecheckExecutor should emit debug information on system out, false
     *     otherwise
     */
    boolean shouldEmitDebugInfo();
}
