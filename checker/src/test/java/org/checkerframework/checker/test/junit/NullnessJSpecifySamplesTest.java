package org.checkerframework.checker.test.junit;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.checkerframework.framework.test.TypecheckResult;
import org.checkerframework.framework.test.diagnostics.DiagnosticKind;
import org.checkerframework.framework.test.diagnostics.TestDiagnostic;
import org.checkerframework.javacutil.BugInCF;
import org.junit.runners.Parameterized.Parameters;

/**
 * JUnit tests for the Nullness Checker -- test the JSpecify samples.
 *
 * <p>Requirements:
 *
 * <ul>
 *   <li>Java 9 or later
 *   <li>Clone and build https://github.com/jspecify/jspecify in a sibling directory of the Checker
 *       Framework.
 * </ul>
 *
 * To run this test:
 *
 * <pre>{@code
 * ./gradlew :checker:NullnessJSpecifySamples
 * }</pre>
 */
public class NullnessJSpecifySamplesTest extends CheckerFrameworkPerDirectoryTest {

  /**
   * Create a NullnessJSpecifySamplesTest.
   *
   * @param testFiles the files containing test code, which will be type-checked
   */
  public NullnessJSpecifySamplesTest(List<File> testFiles) {
    super(
        testFiles,
        org.checkerframework.checker.nullness.NullnessChecker.class,
        "../../../jspecify/samples",
        Collections.singletonList("../../jspecify/build/libs/jspecify-0.1.0-SNAPSHOT.jar"),
        "-Anomsgtext");
  }

  @Parameters
  public static String[] getTestDirs() {
    return new String[] {"../../../jspecify/samples"};
  }

  @Override
  public TypecheckResult adjustTypecheckResult(TypecheckResult testResult) {
    // The "all*" variables are a copy that contains everything.
    // This method removes from the non-all* variables.
    // These are JSpecify diagnostics.
    List<TestDiagnostic> missingDiagnostics = testResult.getMissingDiagnostics();
    List<TestDiagnostic> allMissingDiagnostics =
        Collections.unmodifiableList(new ArrayList<>(missingDiagnostics));
    // These are Checker Framework diagnostics.
    List<TestDiagnostic> unexpectedDiagnostics = testResult.getUnexpectedDiagnostics();
    List<TestDiagnostic> allUnexpectedDiagnostics =
        Collections.unmodifiableList(new ArrayList<>(unexpectedDiagnostics));

    for (TestDiagnostic missing : allMissingDiagnostics) {
      unexpectedDiagnostics.removeIf(unexpected -> matches(missing, unexpected));
    }
    for (TestDiagnostic unexpected : allUnexpectedDiagnostics) {
      missingDiagnostics.removeIf(missing -> matches(missing, unexpected));
    }
    missingDiagnostics.removeIf(
        missing -> missing.getMessage().equals("jspecify_unrecognized_location"));
    missingDiagnostics.removeIf(
        missing -> missing.getMessage().equals("jspecify_nullness_not_enough_information"));
    missingDiagnostics.removeIf(
        // Currently, the only conflict involves @NullnessUnspecified.
        missing -> missing.getMessage().equals("jspecify_conflicting_annotations"));

    return testResult;
  }

  /**
   * Returns true if {@code cfDiagnostic} being issued fulfils the expectation that {@code
   * jspecifyDiagnostic} should be issued.
   *
   * @param jspecifyDiagnostic an expected JSpecify diagnostic
   * @param cfDiagnostic an actual javacdiagnostic
   * @return true if {@code actual} fulfills an expectation to see {@code expected}
   */
  private static boolean matches(TestDiagnostic jspecifyDiagnostic, TestDiagnostic cfDiagnostic) {
    assert jspecifyDiagnostic.getKind() == DiagnosticKind.JSpecify
        : "bad JSpecify diagnostic " + jspecifyDiagnostic;
    assert cfDiagnostic.getKind() != DiagnosticKind.JSpecify : "bad CF diagnostic " + cfDiagnostic;

    if (!(jspecifyDiagnostic.getFilename().equals(cfDiagnostic.getFilename())
        && jspecifyDiagnostic.getLineNumber() == cfDiagnostic.getLineNumber())) {
      return false;
    }

    // The JSpecify diagnostics are documented at
    // https://github.com/jspecify/jspecify/blob/main/samples/README.md#syntax .
    switch (jspecifyDiagnostic.getMessage()) {
      case "jspecify_conflicting_annotations":
        throw new BugInCF("jspecifyMatches(%s, %s)", jspecifyDiagnostic, cfDiagnostic);

      case "jspecify_unrecognized_location":
        return true;

      case "jspecify_nullness_intrinsically_not_nullable":
        switch (cfDiagnostic.getMessage()) {
          case "nullness.on.constructor":
          case "nullness.on.enum":
          case "nullness.on.outer":
          case "nullness.on.primitive":
          case "nullness.on.receiver":
          case "nullness.on.supertype":
            return true;
          default:
            return false;
        }

      case "jspecify_nullness_mismatch":
      case "jspecify_nullness_not_enough_information":
        switch (cfDiagnostic.getMessage()) {
          case "argument":
          case "assignment":
          case "condition.nullable":
          case "dereference.of.nullable":
          case "initialization.field.uninitialized":
          case "locking.nullable":
          case "override.param":
          case "override.return":
          case "return":
          case "type.argument":
          case "unboxing.of.nullable":
            return true;
          default:
            return false;
        }

      default:
        throw new BugInCF("Unexpected JSpecify diagnostic: " + jspecifyDiagnostic.getMessage());
    }
  }
}
