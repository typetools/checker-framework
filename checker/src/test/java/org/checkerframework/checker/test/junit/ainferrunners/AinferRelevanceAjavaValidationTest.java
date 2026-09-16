package org.checkerframework.checker.test.junit.ainferrunners;

import java.io.File;
import java.util.List;
import org.checkerframework.checker.testchecker.ainfer.AinferRelevanceTestChecker;
import org.checkerframework.framework.test.AinferValidatePerDirectoryTest;
import org.junit.experimental.categories.Category;
import org.junit.runners.Parameterized.Parameters;

/**
 * Tests whole-program type inference with ajava files, for a checker that declares
 * {@code @RelevantJavaTypes}. This test is the second pass, which ensures that with the ajava files
 * in place, the errors that those annotations remove are no longer issued.
 *
 * <p>This test fails if inference discards an annotation that is written on a relevant type, such
 * as {@code int} (which the checker lists) or {@code String} (which is relevant because it is a
 * subtype of the listed type {@code CharSequence}).
 */
@Category(AinferRelevanceAjavaGenerationTest.class)
public class AinferRelevanceAjavaValidationTest extends AinferValidatePerDirectoryTest {

  /**
   * @param testFiles the files containing test code, which will be type-checked
   */
  public AinferRelevanceAjavaValidationTest(List<File> testFiles) {
    super(
        testFiles,
        AinferRelevanceTestChecker.class,
        "relevance",
        "ainfer-relevance/annotated",
        AinferRelevanceAjavaGenerationTest.class,
        ajavaArgFromFiles(testFiles, "relevance"),
        "-Awarns");
  }

  @Parameters
  public static String[] getTestDirs() {
    return new String[] {"ainfer-relevance/annotated/"};
  }
}
