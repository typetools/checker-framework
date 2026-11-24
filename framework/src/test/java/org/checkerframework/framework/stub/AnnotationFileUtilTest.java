package org.checkerframework.framework.stub;

import static org.checkerframework.framework.stub.AnnotationFileTestUtil.write;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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

  /** Tests that an archive whose file name extension is upper case is read as an archive. */
  @Test
  public void upperCaseArchiveExtension() throws IOException {
    File zip = writeZip(new File(tmp.getRoot(), "STUBS.ZIP"), "java/lang/String.astub");
    Assert.assertEquals(
        Arrays.asList(zip.getPath() + "!java/lang/String.astub"),
        descriptions(zip, AnnotationFileType.COMMAND_LINE_STUB));

    File jar = writeZip(new File(tmp.getRoot(), "STUBS.JAR"), "java/lang/String.astub");
    Assert.assertEquals(
        Arrays.asList(jar.getPath() + "!java/lang/String.astub"),
        descriptions(jar, AnnotationFileType.COMMAND_LINE_STUB));
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

  /** Tests that a hidden directory that is named explicitly is searched. */
  @Test
  public void explicitHiddenDirectoryIsSearched() throws IOException {
    File dir = tmp.newFolder(".stubdir");
    File a = write(new File(dir, "A.astub"), "package p;");
    Assert.assertEquals(
        Arrays.asList(a.getAbsolutePath()),
        descriptions(dir, AnnotationFileType.COMMAND_LINE_STUB));
  }

  /** Tests that each archive within a directory is passed to the skipped action. */
  @Test
  public void archiveInDirectoryIsReported() throws IOException {
    File dir = tmp.newFolder("stubdir");
    write(new File(dir, "A.astub"), "package p;");
    File backup = writeZip(new File(dir, "backup.zip"), "A.astub");
    File lib = writeZip(new File(dir, "sub/lib.jar"), "B.astub");

    List<File> skipped = new ArrayList<>();
    AnnotationFileUtil.forEachAnnotationFile(
        dir, AnnotationFileType.COMMAND_LINE_STUB, resource -> {}, skipped::add);
    Assert.assertEquals(Arrays.asList(backup, lib), skipped);
  }

  /**
   * Tests that a directory that cannot be listed, within a named directory, is passed to the
   * skipped action rather than to the action.
   */
  @Test
  public void unlistableDirectoryInDirectoryIsReported() throws IOException {
    File dir = tmp.newFolder("stubdir");
    File a = write(new File(dir, "A.astub"), "package p;");
    File sub = new File(dir, "sub");
    write(new File(sub, "B.astub"), "package p;");
    // Making a directory unreadable has no effect for the root user.
    Assume.assumeTrue(sub.setReadable(false));
    try {
      Assume.assumeTrue(sub.listFiles() == null);
      List<String> used = new ArrayList<>();
      List<File> skipped = new ArrayList<>();
      AnnotationFileUtil.forEachAnnotationFile(
          dir,
          AnnotationFileType.COMMAND_LINE_STUB,
          resource -> used.add(resource.getDescription()),
          skipped::add);
      Assert.assertEquals(Arrays.asList(a.getAbsolutePath()), used);
      Assert.assertEquals(Arrays.asList(sub), skipped);
    } finally {
      sub.setReadable(true);
    }
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

  /**
   * Tests that the entries in a zip archive are looked up by the end of their names, and that a
   * hidden entry is omitted.
   */
  @Test
  public void archiveEntriesInZipArchive() throws IOException {
    File zip =
        writeZip(
            new File(tmp.getRoot(), "ajava.zip"),
            "p/Foo-checker.ajava",
            "Bar-checker.ajava",
            "p/Foo-otherchecker.ajava",
            "__MACOSX/p/._Foo-checker.ajava");

    Map<String, List<AnnotationFileResource>> entries =
        AnnotationFileUtil.archiveEntries(zip, "-checker.ajava");
    Assert.assertEquals(
        new HashSet<>(Arrays.asList("p/Foo-checker.ajava", "Bar-checker.ajava")), entries.keySet());
    List<AnnotationFileResource> fooEntries = entries.get("p/Foo-checker.ajava");
    Assert.assertNotNull(fooEntries);
    Assert.assertEquals(1, fooEntries.size());
    AnnotationFileResource entry = fooEntries.get(0);
    Assert.assertEquals(zip.getPath() + "!p/Foo-checker.ajava", entry.getDescription());
    try (InputStream in = entry.getInputStream()) {
      Assert.assertEquals(
          "package java.lang;", new String(in.readAllBytes(), StandardCharsets.UTF_8));
    }
  }

  /**
   * Tests that archive entries are keyed by their normalized names, that each resource retains its
   * entry's original name, that entries with the same normalized name are all returned, and that an
   * entry outside the archive's root is omitted.
   */
  @Test
  public void archiveEntriesAreKeyedByNormalizedName() throws IOException {
    File zip =
        writeZip(
            new File(tmp.getRoot(), "ajava.zip"),
            "./p/Foo-checker.ajava",
            "q/./r/../Bar-checker.ajava",
            "q/Bar-checker.ajava",
            "../Baz-checker.ajava");

    Map<String, List<AnnotationFileResource>> entries =
        AnnotationFileUtil.archiveEntries(zip, "-checker.ajava");
    Assert.assertEquals(
        new HashSet<>(Arrays.asList("p/Foo-checker.ajava", "q/Bar-checker.ajava")),
        entries.keySet());

    List<AnnotationFileResource> fooEntries = entries.get("p/Foo-checker.ajava");
    Assert.assertEquals(1, fooEntries.size());
    Assert.assertEquals(
        zip.getPath() + "!./p/Foo-checker.ajava", fooEntries.get(0).getDescription());
    try (InputStream in = fooEntries.get(0).getInputStream()) {
      Assert.assertEquals(
          "package java.lang;", new String(in.readAllBytes(), StandardCharsets.UTF_8));
    }

    List<String> barDescriptions = new ArrayList<>();
    for (AnnotationFileResource resource : entries.get("q/Bar-checker.ajava")) {
      barDescriptions.add(resource.getDescription());
    }
    Assert.assertEquals(
        Arrays.asList(
            zip.getPath() + "!q/./r/../Bar-checker.ajava", zip.getPath() + "!q/Bar-checker.ajava"),
        barDescriptions);
  }

  /** Tests the normalization of archive entry names. */
  @Test
  public void normalizeEntryName() {
    Assert.assertEquals("p/Foo.ajava", AnnotationFileUtil.normalizeEntryName("p/Foo.ajava"));
    Assert.assertEquals("p/Foo.ajava", AnnotationFileUtil.normalizeEntryName("./p/Foo.ajava"));
    Assert.assertEquals("p/Foo.ajava", AnnotationFileUtil.normalizeEntryName("/p//Foo.ajava"));
    Assert.assertEquals("p/Foo.ajava", AnnotationFileUtil.normalizeEntryName("p/q/../Foo.ajava"));
    Assert.assertNull(AnnotationFileUtil.normalizeEntryName("../Foo.ajava"));
    Assert.assertNull(AnnotationFileUtil.normalizeEntryName("p/../../Foo.ajava"));
  }

  /** Tests that an archive that cannot be opened yields an exception. */
  @Test
  public void archiveEntriesInUnreadableArchive() throws IOException {
    File notZip = write(new File(tmp.getRoot(), "not-an-archive.zip"), "package p;");

    Assert.assertThrows(
        IOException.class, () -> AnnotationFileUtil.archiveEntries(notZip, "-checker.ajava"));
  }

  /**
   * Tests that a relative location that exists only under the {@code test.src} directory, such as
   * an ajava archive in a jtreg test, is found in that directory.
   */
  @Test
  public void findFileSystemLocationInTestSrc() throws IOException {
    File zip = writeZip(new File(tmp.getRoot(), "ajava-for-test-src.zip"), "p/Foo-checker.ajava");
    File absent = new File(tmp.getRoot(), "absent.zip");
    String oldTestSrc = System.getProperty("test.src");
    try {
      System.clearProperty("test.src");
      Assert.assertNull(AnnotationFileUtil.findFileSystemLocation(zip.getName()));

      System.setProperty("test.src", tmp.getRoot().getPath());
      File found = AnnotationFileUtil.findFileSystemLocation(zip.getName());
      Assert.assertEquals(zip, found);
      Assert.assertNotNull(
          AnnotationFileUtil.archiveEntries(found, "-checker.ajava").get("p/Foo-checker.ajava"));
      // An absolute path is not looked for relative to test.src.
      Assert.assertNull(AnnotationFileUtil.findFileSystemLocation(absent.getAbsolutePath()));
    } finally {
      if (oldTestSrc == null) {
        System.clearProperty("test.src");
      } else {
        System.setProperty("test.src", oldTestSrc);
      }
    }
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
