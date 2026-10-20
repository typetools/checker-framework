package org.checkerframework.framework.stub;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.checkerframework.framework.stub.AnnotationFileUtil.AnnotationFileType;
import org.junit.Assert;
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
   * Returns the descriptions of the annotation files found at the given location, in order.
   *
   * @param location a file, directory, or archive
   * @param fileType the file type to collect
   * @return the descriptions of the files that {@link AnnotationFileUtil#allAnnotationFiles} found
   */
  private static List<String> descriptions(File location, AnnotationFileType fileType) {
    List<AnnotationFileResource> resources =
        AnnotationFileUtil.allAnnotationFiles(location.getPath(), fileType);
    Assert.assertNotNull(resources);
    List<String> result = new ArrayList<>(resources.size());
    for (AnnotationFileResource resource : resources) {
      result.add(resource.getDescription());
    }
    return result;
  }

  /** Tests that a zip archive contributes the files in it whose names match the file type. */
  @Test
  public void stubFilesInZipArchive() throws IOException {
    File zip = tmp.newFile("stubs.zip");
    try (OutputStream os = Files.newOutputStream(zip.toPath());
        ZipOutputStream zos = new ZipOutputStream(os)) {
      for (String name : Arrays.asList("java/lang/String.astub", "README.txt", "Other.ajava")) {
        zos.putNextEntry(new ZipEntry(name));
        zos.write("package java.lang;".getBytes(StandardCharsets.UTF_8));
        zos.closeEntry();
      }
    }

    Assert.assertEquals(
        Arrays.asList(zip.getPath() + "!java/lang/String.astub"),
        descriptions(zip, AnnotationFileType.COMMAND_LINE_STUB));
    Assert.assertEquals(
        Arrays.asList(zip.getPath() + "!Other.ajava"), descriptions(zip, AnnotationFileType.AJAVA));
  }

  /**
   * Tests that a file named explicitly is used even if its name does not end with {@code .astub}.
   */
  @Test
  public void explicitFileIsUsedRegardlessOfName() throws IOException {
    File stub = write(new File(tmp.getRoot(), "my-stubs.txt"), "package java.lang;");

    Assert.assertEquals(
        Arrays.asList(stub.getAbsolutePath()),
        descriptions(stub, AnnotationFileType.COMMAND_LINE_STUB));
  }

  /** Tests that a directory contributes only the files in it whose names match the file type. */
  @Test
  public void directoryUsesOnlyFilesWithMatchingName() throws IOException {
    File dir = tmp.newFolder("stubdir");
    File a = write(new File(dir, "A.astub"), "package p;");
    write(new File(dir, "notes.txt"), "not a stub file");
    File b = write(new File(dir, "sub/B.astub"), "package p;");
    write(new File(dir, "sub/C.ajava"), "package p;");

    Assert.assertEquals(
        Arrays.asList(a.getAbsolutePath(), b.getAbsolutePath()),
        descriptions(dir, AnnotationFileType.COMMAND_LINE_STUB));
  }

  /** Tests that a nonexistent location yields null. */
  @Test
  public void nonexistentLocation() {
    Assert.assertNull(
        AnnotationFileUtil.allAnnotationFiles(
            new File(tmp.getRoot(), "does-not-exist.astub").getPath(),
            AnnotationFileType.COMMAND_LINE_STUB));
  }
}
