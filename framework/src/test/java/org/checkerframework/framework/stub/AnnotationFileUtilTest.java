package org.checkerframework.framework.stub;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import org.checkerframework.framework.stub.AnnotationFileUtil.AnnotationFileType;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/** Tests for {@link AnnotationFileUtil#allAnnotationFiles}. */
public class AnnotationFileUtilTest {

  /** A temporary directory, deleted after each test. */
  @Rule public TemporaryFolder tmp = new TemporaryFolder();

  /**
   * Creates a file with the given contents.
   *
   * @param file the file to create
   * @param contents the contents of the file
   * @return {@code file}
   * @throws IOException if the file cannot be written
   */
  private static File write(File file, String contents) throws IOException {
    File parent = file.getParentFile();
    if (parent != null) {
      Files.createDirectories(parent.toPath());
    }
    Files.write(file.toPath(), contents.getBytes(StandardCharsets.UTF_8));
    return file;
  }

  /**
   * Tests that a named archive that cannot be opened yields a resource whose contents cannot be
   * read, rather than being treated as empty.
   */
  @Test
  public void corruptArchive() throws IOException {
    File jar = write(new File(tmp.getRoot(), "stubs.jar"), "not a jar file");

    List<AnnotationFileResource> resources =
        AnnotationFileUtil.allAnnotationFiles(jar.getPath(), AnnotationFileType.COMMAND_LINE_STUB);
    Assert.assertNotNull(resources);
    Assert.assertEquals(1, resources.size());
    Assert.assertEquals(jar.getPath(), resources.get(0).getDescription());
    Assert.assertThrows(IOException.class, () -> resources.get(0).getInputStream());
  }

  /**
   * Tests that a named directory that cannot be listed yields a resource whose contents cannot be
   * read, rather than being treated as empty.
   */
  @Test
  public void unlistableDirectory() throws IOException {
    File dir = tmp.newFolder("stubs");
    write(new File(dir, "A.astub"), "class A {}");
    // Making a directory unreadable has no effect for the root user.
    Assume.assumeTrue(dir.setReadable(false));
    try {
      Assume.assumeTrue(dir.listFiles() == null);
      List<AnnotationFileResource> resources =
          AnnotationFileUtil.allAnnotationFiles(
              dir.getPath(), AnnotationFileType.COMMAND_LINE_STUB);
      Assert.assertNotNull(resources);
      Assert.assertEquals(1, resources.size());
      Assert.assertEquals(dir.getPath(), resources.get(0).getDescription());
      Assert.assertThrows(IOException.class, () -> resources.get(0).getInputStream());
    } finally {
      dir.setReadable(true);
    }
  }
}
