package org.checkerframework.framework.stub;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import org.checkerframework.framework.stub.AnnotationFileUtil.AnnotationFileType;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/** Tests for {@link JarEntryAnnotationFileResource}. */
public class JarEntryAnnotationFileResourceTest {

  /** A temporary directory, deleted after each test. */
  @Rule public TemporaryFolder tmp = new TemporaryFolder();

  /**
   * Creates a jar archive whose entries have the given names. Each entry's contents is its name.
   *
   * @param jar the archive to create
   * @param entryNames the names of the entries
   * @return {@code jar}
   * @throws IOException if the archive cannot be written
   */
  private static File writeJar(File jar, String... entryNames) throws IOException {
    try (OutputStream os = Files.newOutputStream(jar.toPath());
        JarOutputStream jos = new JarOutputStream(os)) {
      for (String entryName : entryNames) {
        jos.putNextEntry(new JarEntry(entryName));
        jos.write(entryName.getBytes(StandardCharsets.UTF_8));
        jos.closeEntry();
      }
    }
    return jar;
  }

  /**
   * Returns the contents of the given resource.
   *
   * @param resource a resource
   * @return the contents of {@code resource}
   * @throws IOException if the resource cannot be read
   */
  private static String contents(AnnotationFileResource resource) throws IOException {
    try (InputStream in = resource.getInputStream()) {
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  /**
   * Tests that every stub file in a jar archive can be read, after the archive has been listed and
   * in any order.
   */
  @Test
  public void readEntriesInJarArchive() throws IOException {
    File jar = writeJar(new File(tmp.getRoot(), "stubs.jar"), "a/A.astub", "B.astub", "C.txt");

    List<AnnotationFileResource> resources =
        AnnotationFileUtil.allAnnotationFiles(jar.getPath(), AnnotationFileType.COMMAND_LINE_STUB);
    Assert.assertNotNull(resources);
    Assert.assertEquals(2, resources.size());
    Assert.assertEquals(jar.getPath() + "!a/A.astub", resources.get(0).getDescription());
    Assert.assertEquals(jar.getPath() + "!B.astub", resources.get(1).getDescription());
    Assert.assertEquals("B.astub", contents(resources.get(1)));
    Assert.assertEquals("a/A.astub", contents(resources.get(0)));
    // Reading an entry a second time works, because each read opens the archive anew.
    Assert.assertEquals("a/A.astub", contents(resources.get(0)));
  }

  /** Tests that reading a nonexistent entry throws an IOException. */
  @Test
  public void readMissingEntry() throws IOException {
    File jar = writeJar(new File(tmp.getRoot(), "stubs.jar"), "A.astub");

    AnnotationFileResource resource = new JarEntryAnnotationFileResource(jar, "Missing.astub");
    Assert.assertThrows(IOException.class, resource::getInputStream);
  }
}
