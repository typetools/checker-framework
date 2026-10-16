package org.checkerframework.checker.test.junit.ainferrunners;

import java.io.File;
import java.util.List;
import org.checkerframework.checker.resourceleak.ResourceLeakChecker;
import org.checkerframework.framework.test.AinferValidatePerDirectoryTest;
import org.junit.experimental.categories.Category;
import org.junit.runners.Parameterized.Parameters;

/**
 * Tests RLC-specific inference features with ajava files. This test is the second pass, which
 * ensures that with the ajava files in place, the errors that those annotations remove are no
 * longer issued.
 */
@Category(AinferResourceLeakAjavaGenerationTest.class)
public class AinferResourceLeakAjavaValidationTest extends AinferValidatePerDirectoryTest {

  /**
   * @param testFiles the files containing test code, which will be type-checked
   */
  public AinferResourceLeakAjavaValidationTest(List<File> testFiles) {
    super(
        testFiles,
        ResourceLeakChecker.class,
        "resourceleak",
        "ainfer-resourceleak/annotated",
        AinferResourceLeakAjavaGenerationTest.class,
        ajavaArgFromFiles(testFiles, "resourceleak"),
        "-Awarns");
  }

  @Parameters
  public static String[] getTestDirs() {
    return new String[] {"ainfer-resourceleak/annotated/"};
  }
}
