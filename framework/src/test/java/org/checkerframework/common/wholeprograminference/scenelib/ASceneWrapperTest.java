package org.checkerframework.common.wholeprograminference.scenelib;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.checkerframework.afu.scenelib.el.AScene;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.wholeprograminference.WholeProgramInference.OutputFormat;
import org.checkerframework.common.wholeprograminference.WholeProgramInferenceScenesStorage.AnnotationsInContexts;
import org.checkerframework.javacutil.UserError;
import org.junit.Assert;
import org.junit.Test;

/** Tests for {@link ASceneWrapper}. */
public class ASceneWrapperTest {

  /**
   * Tests that {@link ASceneWrapper#writeToFile} reports a problem, rather than silently
   * continuing, when the output file cannot be deleted. If it continued, a stale output file would
   * survive and would be read back on the next iteration of whole-program inference.
   */
  @Test
  public void testUndeletableOutputFile() throws IOException {
    Path tempDir = Files.createTempDirectory("ASceneWrapperTest");
    // A non-empty directory cannot be deleted.
    Path jaifPath = tempDir.resolve("out.jaif");
    Files.createDirectory(jaifPath);
    Files.createFile(jaifPath.resolve("occupant"));

    // The scene is empty, so nothing would be written even if deletion succeeded.
    ASceneWrapper scene = new ASceneWrapper(new AScene());
    BaseTypeChecker checker = new BaseTypeChecker() {};

    try {
      scene.writeToFile(
          jaifPath.toString(), new AnnotationsInContexts(), OutputFormat.JAIF, checker);
      Assert.fail("writeToFile did not report the failure to delete " + jaifPath);
    } catch (UserError e) {
      Assert.assertTrue(e.getMessage(), e.getMessage().contains(jaifPath.toString()));
    } finally {
      Files.delete(jaifPath.resolve("occupant"));
      Files.delete(jaifPath);
      Files.delete(tempDir);
    }
  }

  /** Tests {@link ASceneWrapper#replaceJaifExtension}. */
  @Test
  public void testReplaceJaifExtension() {
    Assert.assertEquals(
        "build/wpi/Foo-org.example.MyChecker.astub",
        ASceneWrapper.replaceJaifExtension("build/wpi/Foo.jaif", "-org.example.MyChecker.astub"));

    // Only the extension is replaced; ".jaif" within a directory name is left alone.
    Assert.assertEquals(
        "build/whole-program-inference.jaif/Foo-org.example.MyChecker.astub",
        ASceneWrapper.replaceJaifExtension(
            "build/whole-program-inference.jaif/Foo.jaif", "-org.example.MyChecker.astub"));

    // The base name may itself contain ".jaif".
    Assert.assertEquals(
        "build/wpi/a.jaif.b-org.example.MyChecker.astub",
        ASceneWrapper.replaceJaifExtension(
            "build/wpi/a.jaif.b.jaif", "-org.example.MyChecker.astub"));
  }
}
