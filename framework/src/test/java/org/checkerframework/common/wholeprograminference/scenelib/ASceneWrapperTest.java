package org.checkerframework.common.wholeprograminference.scenelib;

import org.junit.Assert;
import org.junit.Test;

/** Tests for {@link ASceneWrapper}. */
public class ASceneWrapperTest {

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
