package org.checkerframework.checker.test.junit.ainferrunners;

import java.io.File;
import java.util.List;
import org.checkerframework.checker.resourceleak.ResourceLeakChecker;
import org.checkerframework.framework.test.AinferGeneratePerDirectoryTest;
import org.junit.experimental.categories.Category;
import org.junit.runners.Parameterized.Parameters;

/**
 * Tests RLC-specific inference features with the aid of ajava files. This test is the first pass on
 * the test data, which generates the ajava files.
 *
 * <p>This does not run WPI; it just runs one round of type-checking with {@code -Ainfer} enabled.
 *
 * <p>IMPORTANT: The errors captured in the tests located in tests/ainfer-resourceleak/ are not
 * relevant. The meaning of this test class is to test if the generated ajava files are similar to
 * the expected ones. The errors on .java files must be ignored.
 */
@Category(AinferResourceLeakAjavaGenerationTest.class)
public class AinferResourceLeakAjavaGenerationTest extends AinferGeneratePerDirectoryTest {

  /**
   * @param testFiles the files containing test code, which will be type-checked
   */
  public AinferResourceLeakAjavaGenerationTest(List<File> testFiles) {
    super(
        testFiles,
        ResourceLeakChecker.class,
        "ainfer-resourceleak/non-annotated",
        "-Ainfer=ajava",
        // "-Aajava=tests/ainfer-resourceleak/input-annotation-files/",
        "-Awarns");
  }

  @Parameters
  public static String[] getTestDirs() {
    return new String[] {"ainfer-resourceleak/non-annotated"};
  }
}
