package org.checkerframework.framework.test;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import org.checkerframework.framework.test.diagnostics.TestDiagnostic;
import org.checkerframework.framework.test.diagnostics.TestDiagnosticUtils;
import org.checkerframework.javacutil.SystemUtil;

/**
 * Represents the test results from typechecking one or more java files using the given
 * TestConfiguration.
 */
public class TypecheckResult {
    private final TestConfiguration configuration;
    private final CompilationResult compilationResult;
    private final List<TestDiagnostic> expectedDiagnostics;

    private final boolean testFailed;

    private final List<TestDiagnostic> missingDiagnostics;
    private final List<TestDiagnostic> unexpectedDiagnostics;

    protected TypecheckResult(
            TestConfiguration configuration,
            CompilationResult compilationResult,
            List<TestDiagnostic> expectedDiagnostics,
            boolean testFailed,
            List<TestDiagnostic> missingDiagnostics,
            List<TestDiagnostic> unexpectedDiagnostics) {
        this.configuration = configuration;
        this.compilationResult = compilationResult;
        this.expectedDiagnostics = expectedDiagnostics;
        this.testFailed = testFailed;
        this.missingDiagnostics = missingDiagnostics;
        this.unexpectedDiagnostics = unexpectedDiagnostics;
    }

    public TestConfiguration getConfiguration() {
        return configuration;
    }

    public CompilationResult getCompilationResult() {
        return compilationResult;
    }

    public List<Diagnostic<? extends JavaFileObject>> getActualDiagnostics() {
        return compilationResult.getDiagnostics();
    }

    public List<TestDiagnostic> getExpectedDiagnostics() {
        return expectedDiagnostics;
    }

    public boolean didTestFail() {
        return testFailed;
    }

    public List<TestDiagnostic> getMissingDiagnostics() {
        return missingDiagnostics;
    }

    public List<TestDiagnostic> getUnexpectedDiagnostics() {
        return unexpectedDiagnostics;
    }

    public List<String> getErrorHeaders() {
        List<String> errorHeaders = new ArrayList<>();

        // none of these should be true if the test didn't fail
        if (testFailed) {
            if (compilationResult.compiledWithoutError() && !expectedDiagnostics.isEmpty()) {
                errorHeaders.add(
                        "The test run was expected to issue errors/warnings, but it did not.");

            } else if (!compilationResult.compiledWithoutError() && expectedDiagnostics.isEmpty()) {
                errorHeaders.add(
                        "The test run was not expected to issue errors/warnings, but it did.");
            }

            if (!unexpectedDiagnostics.isEmpty() || !missingDiagnostics.isEmpty()) {
                int numExpected = expectedDiagnostics.size();
                int numFound = numExpected - missingDiagnostics.size();
                errorHeaders.add(
                        numFound
                                + " out of "
                                + numExpected
                                + " expected diagnostics "
                                + (numFound == 1 ? "was" : "were")
                                + " found.");
            }
        }

        return errorHeaders;
    }

    /**
     * Summarize unexpected and missing diagnostics.
     *
     * @return summary of failures
     */
    public String summarize() {
        if (testFailed) {
            StringJoiner summaryBuilder = new StringJoiner(System.lineSeparator());
            summaryBuilder.add(SystemUtil.joinLines(getErrorHeaders()));

            if (!unexpectedDiagnostics.isEmpty()) {
                summaryBuilder.add(
                        unexpectedDiagnostics.size() == 1
                                ? "1 unexpected diagnostic was found:"
                                : unexpectedDiagnostics.size()
                                        + " unexpected diagnostics were found:");

                for (TestDiagnostic unexpected : unexpectedDiagnostics) {
                    summaryBuilder.add(unexpected.toString());
                }
            }

            if (!missingDiagnostics.isEmpty()) {
                summaryBuilder.add(
                        missingDiagnostics.size() == 1
                                ? "1 expected diagnostic was not found:"
                                : missingDiagnostics.size()
                                        + " expected diagnostics were not found:");

                for (TestDiagnostic missing : missingDiagnostics) {
                    summaryBuilder.add(missing.toString());
                }
            }

            summaryBuilder.add(
                    "While type-checking "
                            + TestUtilities.summarizeSourceFiles(
                                    configuration.getTestSourceFiles()));
            return summaryBuilder.toString();
        }

        return "";
    }

    public static TypecheckResult fromCompilationResults(
            TestConfiguration configuration,
            CompilationResult result,
            List<TestDiagnostic> expectedDiagnostics) {

        boolean usingAnomsgtxt = configuration.getOptions().containsKey("-Anomsgtext");
        final Set<TestDiagnostic> actualDiagnostics =
                TestDiagnosticUtils.fromJavaxDiagnosticList(
                        result.getDiagnostics(), usingAnomsgtxt);

        final Set<TestDiagnostic> unexpectedDiagnostics = new LinkedHashSet<>();
        unexpectedDiagnostics.addAll(actualDiagnostics);
        unexpectedDiagnostics.removeAll(expectedDiagnostics);

        final List<TestDiagnostic> missingDiagnostics = new ArrayList<>(expectedDiagnostics);
        missingDiagnostics.removeAll(actualDiagnostics);

        boolean testFailed = !unexpectedDiagnostics.isEmpty() || !missingDiagnostics.isEmpty();

        return new TypecheckResult(
                configuration,
                result,
                expectedDiagnostics,
                testFailed,
                missingDiagnostics,
                new ArrayList<>(unexpectedDiagnostics));
    }
}
