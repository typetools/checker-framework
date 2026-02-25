package org.checkerframework.framework.test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import org.checkerframework.framework.test.diagnostics.TestDiagnostic;
import org.checkerframework.framework.test.diagnostics.TestDiagnosticUtils;
import org.plumelib.util.StringsPlume;

/**
 * Represents the test results from typechecking one or more Java files using the given
 * TestConfiguration.
 */
public class TypecheckResult {
  /** The test configuration. */
  private final TestConfiguration configuration;

  /** The compilation result. */
  private final CompilationResult compilationResult;

  // In Java 21, can declare the next three fields as SequencedCollection.

  /** The expected diagnostics. */
  private final Collection<TestDiagnostic> expectedDiagnostics;

  /** The diagnostics that were expected but were not issued. */
  private final Collection<TestDiagnostic> missingDiagnostics;

  /** The diagnostics that were issued but were not expected. */
  private final Collection<TestDiagnostic> unexpectedDiagnostics;

  /**
   * Creates a TypecheckResult.
   *
   * @param configuration The test configuration.
   * @param compilationResult The compilation result.
   * @param expectedDiagnostics the expected diagnostics
   * @param missingDiagnostics the diagnostics that were expected but were not issued
   * @param unexpectedDiagnostics the diagnostics that were issued but were not expected
   */
  protected TypecheckResult(
      TestConfiguration configuration,
      CompilationResult compilationResult,
      Collection<TestDiagnostic> expectedDiagnostics,
      Collection<TestDiagnostic> missingDiagnostics,
      Collection<TestDiagnostic> unexpectedDiagnostics) {
    this.configuration = configuration;
    this.compilationResult = compilationResult;
    this.expectedDiagnostics = expectedDiagnostics;
    this.missingDiagnostics = missingDiagnostics;
    this.unexpectedDiagnostics = unexpectedDiagnostics;
  }

  /**
   * Returns the test configuration.
   *
   * @return the test configuration
   */
  public TestConfiguration getConfiguration() {
    return configuration;
  }

  /**
   * Returns the compilation result.
   *
   * @return the compilation result
   */
  public CompilationResult getCompilationResult() {
    return compilationResult;
  }

  /**
   * Returns the actual diagnostics.
   *
   * @return the actual diagnostics
   */
  public Collection<Diagnostic<? extends JavaFileObject>> getActualDiagnostics() {
    return compilationResult.getDiagnostics();
  }

  /**
   * Returns the expected diagnostics.
   *
   * @return the expected diagnostics
   */
  public Collection<TestDiagnostic> getExpectedDiagnostics() {
    return expectedDiagnostics;
  }

  /**
   * Returns true if the test failed.
   *
   * @return true if the test failed
   */
  public boolean didTestFail() {
    return !unexpectedDiagnostics.isEmpty() || !missingDiagnostics.isEmpty();
  }

  /**
   * Returns the diagnostics that were expected but were not issued.
   *
   * @return the diagnostics that were expected but were not issued
   */
  public Collection<TestDiagnostic> getMissingDiagnostics() {
    return missingDiagnostics;
  }

  /**
   * Returns the diagnostics that were issued but were not expected.
   *
   * @return the diagnostics that were issued but were not expected
   */
  public Collection<TestDiagnostic> getUnexpectedDiagnostics() {
    return unexpectedDiagnostics;
  }

  public List<String> getErrorHeaders() {
    List<String> errorHeaders = new ArrayList<>();

    // none of these should be true if the test didn't fail
    if (didTestFail()) {
      if (compilationResult.compiledWithoutError() && !expectedDiagnostics.isEmpty()) {
        errorHeaders.add("The test run was expected to issue errors/warnings, but it did not.");

      } else if (!compilationResult.compiledWithoutError() && expectedDiagnostics.isEmpty()) {
        errorHeaders.add("The test run was not expected to issue errors/warnings, but it did.");
      }

      if (!unexpectedDiagnostics.isEmpty() || !missingDiagnostics.isEmpty()) {
        int numExpected = expectedDiagnostics.size();
        int numFound = numExpected - missingDiagnostics.size();
        errorHeaders.add(
            numFound
                + " out of "
                + StringsPlume.nvPlural(numExpected, "expected diagnostic", "was")
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
    if (!didTestFail()) {
      return "";
    }
    StringJoiner summaryBuilder = new StringJoiner(System.lineSeparator());
    summaryBuilder.add(StringsPlume.joinLines(getErrorHeaders()));

    if (!unexpectedDiagnostics.isEmpty()) {
      int numUnexpected = unexpectedDiagnostics.size();
      if (numUnexpected == 1) {
        summaryBuilder.add("1 unexpected diagnostic was found:");
      } else {
        summaryBuilder.add(numUnexpected + " unexpected diagnostics were found:");
      }

      for (TestDiagnostic unexpected : unexpectedDiagnostics) {
        summaryBuilder.add("  " + unexpected.toString());
      }
    }

    if (!missingDiagnostics.isEmpty()) {
      int numMissing = missingDiagnostics.size();
      summaryBuilder.add(
          StringsPlume.nvPlural(numMissing, "expected diagnostic", "was") + " not found:");

      for (TestDiagnostic missing : missingDiagnostics) {
        summaryBuilder.add("  " + missing.toString());
      }
    }
    return summaryBuilder.toString();
  }

  public static TypecheckResult fromCompilationResults(
      TestConfiguration configuration,
      CompilationResult result,
      Collection<TestDiagnostic> expectedDiagnostics) {

    // We are passing `true` as the `noMsgText` argument because "-Anomsgtext"
    // is always added to the non-JVM options in `TypecheckExecutor.compile`.
    Set<TestDiagnostic> actualDiagnostics =
        TestDiagnosticUtils.fromJavaxDiagnosticList(result.getDiagnostics(), true);

    Set<TestDiagnostic> unexpectedDiagnostics = new LinkedHashSet<>(actualDiagnostics);
    unexpectedDiagnostics.removeAll(expectedDiagnostics);

    Set<TestDiagnostic> missingDiagnostics = new LinkedHashSet<>(expectedDiagnostics);
    missingDiagnostics.removeAll(actualDiagnostics);

    return new TypecheckResult(
        configuration, result, expectedDiagnostics, missingDiagnostics, unexpectedDiagnostics);
  }
}
