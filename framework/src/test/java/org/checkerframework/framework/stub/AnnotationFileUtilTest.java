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

  /** Tests that an archive found by searching a directory is not opened. */
  @Test
  public void archiveInDirectoryIsNotOpened() throws IOException {
    File dir = tmp.newFolder("stubdir");
    File a = write(new File(dir, "A.astub"), "package p;");
    writeZip(new File(dir, "backup.jar"), "A.astub");
    writeZip(new File(dir, "sub/lib.jar"), "B.astub");

    Assert.assertEquals(
        Arrays.asList(a.getAbsolutePath()),
        descriptions(dir, AnnotationFileType.COMMAND_LINE_STUB));
  }

  /** Tests that hidden files, such as macOS metadata files, are not used. */
  @Test
  public void hiddenFilesAreNotUsed() throws IOException {
    File jar =
        writeZip(
            new File(tmp.getRoot(), "stubs.jar"),
            "java/lang/String.astub",
            "__MACOSX/java/lang/._String.astub");
    Assert.assertEquals(
        Arrays.asList(jar.getPath() + "!java/lang/String.astub"),
        descriptions(jar, AnnotationFileType.COMMAND_LINE_STUB));

    File dir = tmp.newFolder("stubdir");
    File a = write(new File(dir, "A.astub"), "package p;");
    write(new File(dir, "._A.astub"), "not a stub file");
    Assert.assertEquals(
        Arrays.asList(a.getAbsolutePath()),
        descriptions(dir, AnnotationFileType.COMMAND_LINE_STUB));
  }

  /** Tests that the files in a hidden directory, or under a hidden archive entry, are not used. */
  @Test
  public void hiddenDirectoriesAreNotSearched() throws IOException {
    File dir = tmp.newFolder("stubdir");
    File a = write(new File(dir, "A.astub"), "package p;");
    write(new File(dir, ".history/A_20231010.astub"), "package p;");
    Assert.assertEquals(
        Arrays.asList(a.getAbsolutePath()),
        descriptions(dir, AnnotationFileType.COMMAND_LINE_STUB));

    File jar =
        writeZip(
            new File(tmp.getRoot(), "stubs.jar"),
            "java/lang/String.astub",
            ".backup/java/lang/String.astub");
    Assert.assertEquals(
        Arrays.asList(jar.getPath() + "!java/lang/String.astub"),
        descriptions(jar, AnnotationFileType.COMMAND_LINE_STUB));
  }

  /** Tests that a "." or ".." component of an archive entry's name does not make it hidden. */
  @Test
  public void currentDirectoryComponentIsNotHidden() throws IOException {
    File jar =
        writeZip(
            new File(tmp.getRoot(), "stubs.jar"),
            "./java/lang/String.astub",
            "java/./lang/../lang/Integer.astub",
            "./java/.hidden/Long.astub");
    Assert.assertEquals(
        Arrays.asList(
            jar.getPath() + "!./java/lang/String.astub",
            jar.getPath() + "!java/./lang/../lang/Integer.astub"),
        descriptions(jar, AnnotationFileType.COMMAND_LINE_STUB));
  }

  /** Tests that a ZIP archive, or an archive whose name is in upper case, is read. */
  @Test
  public void zipAndUpperCaseArchivesAreRead() throws IOException {
    File zip = writeZip(new File(tmp.getRoot(), "stubs.zip"), "java/lang/String.astub");
    Assert.assertEquals(
        Arrays.asList(zip.getPath() + "!java/lang/String.astub"),
        descriptions(zip, AnnotationFileType.COMMAND_LINE_STUB));

    File upperJar = writeZip(new File(tmp.getRoot(), "STUBS.JAR"), "java/lang/String.astub");
    Assert.assertEquals(
        Arrays.asList(upperJar.getPath() + "!java/lang/String.astub"),
        descriptions(upperJar, AnnotationFileType.COMMAND_LINE_STUB));
  }

  /** Tests that a hidden directory that is named explicitly is searched. */
  @Test
  public void explicitHiddenDirectoryIsSearched() throws IOException {
    File dir = tmp.newFolder(".stubdir");
    File a = write(new File(dir, "A.astub"), "package p;");
    Assert.assertEquals(
        Arrays.asList(a.getAbsolutePath()),
        descriptions(dir, AnnotationFileType.COMMAND_LINE_STUB));
  }

  /** Tests that each archive within a directory is passed to the ignored-archive action. */
  @Test
  public void archiveInDirectoryIsReported() throws IOException {
    File dir = tmp.newFolder("stubdir");
    write(new File(dir, "A.astub"), "package p;");
    File backup = writeZip(new File(dir, "backup.jar"), "A.astub");
    File lib = writeZip(new File(dir, "sub/lib.jar"), "B.astub");
    File zip = writeZip(new File(dir, "sub/stubs.zip"), "C.astub");

    List<File> ignoredArchives = new ArrayList<>();
    AnnotationFileUtil.forEachAnnotationFile(
        dir, AnnotationFileType.COMMAND_LINE_STUB, resource -> {}, ignoredArchives::add);
    Assert.assertEquals(Arrays.asList(backup, lib, zip), ignoredArchives);
  }

  /** Tests that a hidden file named explicitly is used if its name indicates its file type. */
  @Test
  public void explicitHiddenFileIsUsed() throws IOException {
    File ajava = write(new File(tmp.getRoot(), ".Foo-checker.ajava"), "package p;");
    File stub = write(new File(tmp.getRoot(), ".Foo.astub"), "package p;");

    Assert.assertEquals(
        Arrays.asList(ajava.getAbsolutePath()), descriptions(ajava, AnnotationFileType.AJAVA));
    Assert.assertEquals(Arrays.asList(), descriptions(ajava, AnnotationFileType.COMMAND_LINE_STUB));
    Assert.assertEquals(
        Arrays.asList(stub.getAbsolutePath()),
        descriptions(stub, AnnotationFileType.COMMAND_LINE_STUB));
  }

  /** Tests that a nonexistent location yields null. */
  @Test
  public void nonexistentLocation() {
    Assert.assertNull(
        AnnotationFileUtil.allAnnotationFiles(
            new File(tmp.getRoot(), "does-not-exist.astub").getPath(),
            AnnotationFileType.COMMAND_LINE_STUB));
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
