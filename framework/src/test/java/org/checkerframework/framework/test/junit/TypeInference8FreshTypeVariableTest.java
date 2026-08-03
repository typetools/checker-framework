package org.checkerframework.framework.test.junit;

import java.io.File;
import java.util.List;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.checkerframework.framework.test.TypecheckResult;
import org.checkerframework.framework.testchecker.util.EvenOddChecker;
import org.junit.Assert;
import org.junit.runners.Parameterized.Parameters;

/**
 * Tests that {@code InferenceFactory.createFreshTypeVariable} tolerates a null {@code upperBound},
 * as its Javadoc promises. {@code Resolution.resolveWithCapture} obtains the upper bound from
 * {@code glb(ai.getBounds().upperBounds())}, which is null when every upper bound of the variable
 * is a use of another inference variable; {@code createFreshTypeVariable} used to dereference it
 * unconditionally.
 *
 * <p>This test asserts only that the checker does not crash inside {@code createFreshTypeVariable}.
 * It deliberately does not assert that the test input type-checks cleanly: resolving such a
 * variable produces a fresh type variable whose {@code Object} upper bound carries no qualifier,
 * which makes a later subtyping check crash. That is a separate defect, so requiring a clean run
 * here would couple this test to it.
 */
public class TypeInference8FreshTypeVariableTest extends CheckerFrameworkPerDirectoryTest {

  /**
   * The name of the method that must not appear in a crash stack trace. A crash inside type
   * argument inference is reported with the stack trace in the diagnostic's message, because {@link
   * CheckerFrameworkPerDirectoryTest} passes {@code -AconvertTypeArgInferenceCrashToWarning=false}.
   */
  private static final String CREATE_FRESH_TYPE_VARIABLE = "createFreshTypeVariable";

  /**
   * @param testFiles the files containing test code, which will be type-checked
   */
  public TypeInference8FreshTypeVariableTest(List<File> testFiles) {
    super(testFiles, EvenOddChecker.class, "typeinference8");
  }

  @Parameters
  public static String[] getTestDirs() {
    return new String[] {"typeinference8"};
  }

  @Override
  public void checkResult(TypecheckResult typecheckResult) {
    for (Diagnostic<? extends JavaFileObject> diagnostic :
        typecheckResult.getCompilationResult().getDiagnostics()) {
      String message = diagnostic.getMessage(null);
      if (message.contains(CREATE_FRESH_TYPE_VARIABLE)) {
        Assert.fail(
            "Type argument inference crashed in "
                + CREATE_FRESH_TYPE_VARIABLE
                + ":"
                + System.lineSeparator()
                + message);
      }
    }
  }
}
