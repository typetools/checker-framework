package org.checkerframework.framework.test.junit;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.StringJoiner;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import org.checkerframework.framework.test.TestConfiguration;
import org.checkerframework.framework.test.TestConfigurationBuilder;
import org.checkerframework.framework.test.TestUtilities;
import org.checkerframework.framework.test.TypecheckExecutor;
import org.checkerframework.framework.test.TypecheckResult;
import org.checkerframework.framework.testchecker.util.EvenOddChecker;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;

/**
 * Tests {@code InferenceFactory.argumentIndex}, which reports that the expression whose type
 * arguments are being inferred is not an argument of the invocation that is the expression's
 * assignment context.
 *
 * <p>Previously, the two {@code InferenceFactory.assignedToExecutable} overloads each left {@code
 * treeIndex} at -1 in that situation and then called {@code getParameterTypes().get(treeIndex)}, so
 * the only diagnostic was an {@code IndexOutOfBoundsException} message such as "Index: -1 Size: 1",
 * which names neither the expression nor the invocation.
 *
 * <p>This test does not check that the test input typechecks; it does not. Type argument inference
 * fails on the test input, and this test checks only that the failure is reported informatively
 * rather than as an out-of-bounds index. If inference is ever made to succeed on a poly expression
 * in the guard of a case of a switch expression, then this test needs a different input that still
 * reaches {@code argumentIndex}'s failure case, or this test should be deleted.
 */
public class TypeInference8ArgumentIndexTest {

  /** The directory containing this test's input file, relative to {@code framework/}. */
  private static final String TEST_DIR = "tests/typeinference8-argumentindex";

  /** Creates a TypeInference8ArgumentIndexTest. */
  public TypeInference8ArgumentIndexTest() {}

  /**
   * Checks that when the expression whose type arguments are being inferred is not an argument of
   * the invocation that is the expression's assignment context, the reported error names both the
   * expression and the invocation.
   */
  @Test
  public void notAnArgumentOfTheInvocation() {
    // The test input uses a `when` clause in a case of a switch expression.
    Assume.assumeTrue(TestUtilities.IS_AT_LEAST_21_JVM);

    TestConfiguration config =
        TestConfigurationBuilder.buildDefaultConfiguration(
            TEST_DIR,
            new File(TEST_DIR, "SwitchExpressionGuard.java"),
            EvenOddChecker.class,
            Collections.emptyList(),
            TestUtilities.getShouldEmitDebugInfo());
    TypecheckResult result = new TypecheckExecutor().runTest(config);

    List<Diagnostic<? extends JavaFileObject>> diagnostics =
        result.getCompilationResult().getDiagnostics();
    for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics) {
      String message = diagnostic.getMessage(null);
      if (message.contains("Not an argument of the invocation.")
          && message.contains("Tree: id(true)")
          && message.contains("Invocation: use(")) {
        return;
      }
    }

    StringJoiner sj = new StringJoiner(System.lineSeparator());
    sj.add("Expected a diagnostic naming the expression and the invocation, but found:");
    for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics) {
      sj.add(diagnostic.getMessage(null));
    }
    Assert.fail(sj.toString());
  }
}
