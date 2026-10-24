package org.checkerframework.framework.stub;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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

  /**
   * Creates a zip archive whose entries have the given names.
   *
   * @param zip the archive to create
   * @param entryNames the names of the entries in the archive
   * @return {@code zip}
   * @throws IOException if the archive cannot be written
   */
  private static File writeZip(File zip, String... entryNames) throws IOException {
    File parent = zip.getParentFile();
    if (parent != null) {
      Files.createDirectories(parent.toPath());
    }
    try (OutputStream os = Files.newOutputStream(zip.toPath());
        ZipOutputStream zos = new ZipOutputStream(os)) {
      for (String name : entryNames) {
        zos.putNextEntry(new ZipEntry(name));
        zos.write("package java.lang;".getBytes(StandardCharsets.UTF_8));
        zos.closeEntry();
      }
    }
    return zip;
  }

  /** Tests that a zip archive contributes the files in it whose names match the file type. */
  @Test
  public void stubFilesInZipArchive() throws IOException {
    File zip =
        writeZip(
            new File(tmp.getRoot(), "stubs.zip"),
            "java/lang/String.astub",
            "README.txt",
            "Other.ajava");

    Assert.assertEquals(
        Arrays.asList(zip.getPath() + "!java/lang/String.astub"),
        descriptions(zip, AnnotationFileType.COMMAND_LINE_STUB));
    Assert.assertEquals(
        Arrays.asList(zip.getPath() + "!Other.ajava"), descriptions(zip, AnnotationFileType.AJAVA));
  }

  /** Tests that the contents of an entry in a zip archive can be read. */
  @Test
  public void readEntryInZipArchive() throws IOException {
    File zip = writeZip(new File(tmp.getRoot(), "stubs.zip"), "java/lang/String.astub");

    List<AnnotationFileResource> resources =
        AnnotationFileUtil.allAnnotationFiles(zip.getPath(), AnnotationFileType.COMMAND_LINE_STUB);
    Assert.assertNotNull(resources);
    Assert.assertEquals(1, resources.size());
    try (InputStream in = resources.get(0).getInputStream()) {
      Assert.assertEquals(
          "package java.lang;", new String(in.readAllBytes(), StandardCharsets.UTF_8));
    }
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

  /** Tests that an archive whose file name extension is upper case is read as an archive. */
  @Test
  public void upperCaseArchiveExtension() throws IOException {
    File zip = writeZip(new File(tmp.getRoot(), "STUBS.ZIP"), "java/lang/String.astub");

    Assert.assertEquals(
        Arrays.asList(zip.getPath() + "!java/lang/String.astub"),
        descriptions(zip, AnnotationFileType.COMMAND_LINE_STUB));
  }

  /** Tests that an archive found by searching a directory is not opened. */
  @Test
  public void archiveInDirectoryIsNotOpened() throws IOException {
    File dir = tmp.newFolder("stubdir");
    File a = write(new File(dir, "A.astub"), "package p;");
    writeZip(new File(dir, "backup.zip"), "A.astub");
    writeZip(new File(dir, "sub/lib.jar"), "B.astub");

    Assert.assertEquals(
        Arrays.asList(a.getAbsolutePath()),
        descriptions(dir, AnnotationFileType.COMMAND_LINE_STUB));
  }

  /** Tests that hidden files, such as macOS metadata files, are not used. */
  @Test
  public void hiddenFilesAreNotUsed() throws IOException {
    File zip =
        writeZip(
            new File(tmp.getRoot(), "stubs.zip"),
            "java/lang/String.astub",
            "__MACOSX/java/lang/._String.astub");
    Assert.assertEquals(
        Arrays.asList(zip.getPath() + "!java/lang/String.astub"),
        descriptions(zip, AnnotationFileType.COMMAND_LINE_STUB));

    File dir = tmp.newFolder("stubdir");
    File a = write(new File(dir, "A.astub"), "package p;");
    write(new File(dir, "._A.astub"), "not a stub file");
    Assert.assertEquals(
        Arrays.asList(a.getAbsolutePath()),
        descriptions(dir, AnnotationFileType.COMMAND_LINE_STUB));
  }

  /** Tests that an ajava file named explicitly is used only if its name ends with ".ajava". */
  @Test
  public void explicitAjavaFileMustNotBeRenamed() throws IOException {
    File ajava = write(new File(tmp.getRoot(), "Foo-checker.ajava"), "package p;");
    File renamed = write(new File(tmp.getRoot(), "Foo-annotations.txt"), "package p;");

    Assert.assertEquals(
        Arrays.asList(ajava.getAbsolutePath()), descriptions(ajava, AnnotationFileType.AJAVA));
    Assert.assertEquals(Arrays.asList(), descriptions(renamed, AnnotationFileType.AJAVA));
  }

  /** Tests that an ajava file named explicitly as a stub file is not used. */
  @Test
  public void explicitAjavaFileIsNotAStubFile() throws IOException {
    File ajava = write(new File(tmp.getRoot(), "Foo-checker.ajava"), "package p;");

    Assert.assertEquals(Arrays.asList(), descriptions(ajava, AnnotationFileType.COMMAND_LINE_STUB));
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
