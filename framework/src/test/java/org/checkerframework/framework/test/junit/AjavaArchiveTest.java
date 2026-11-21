package org.checkerframework.framework.test.junit;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.checkerframework.framework.testchecker.util.EvenOddChecker;
import org.junit.runners.Parameterized.Parameters;

/**
 * JUnit tests for reading annotations from an ajava file in an archive, including the annotations
 * that are read only by tree-aware parsing of the ajava file for the compilation unit being
 * checked.
 */
public class AjavaArchiveTest extends CheckerFrameworkPerDirectoryTest {

  /** The directory whose contents are put in the archive. */
  private static final Path ARCHIVE_CONTENTS =
      Paths.get("tests/ajava-archive/input-annotation-files");

  /** The archive of ajava files, which this test creates. */
  private static final Path ARCHIVE = Paths.get("build/ajava-archive-test/ajava-files.zip");

  /**
   * Creates an AjavaArchiveTest.
   *
   * @param testFiles the files containing test code, which will be type-checked
   */
  public AjavaArchiveTest(List<File> testFiles) {
    super(
        testFiles,
        EvenOddChecker.class,
        "ajava-archive",
        "-Anomsgtext",
        "-Aajava=" + createArchive());
  }

  /**
   * Creates the archive of ajava files from the files in {@link #ARCHIVE_CONTENTS}.
   *
   * @return the path of the archive
   */
  private static String createArchive() {
    try {
      Files.createDirectories(ARCHIVE.getParent());
      List<Path> files;
      try (Stream<Path> paths = Files.walk(ARCHIVE_CONTENTS)) {
        files = paths.filter(Files::isRegularFile).sorted().collect(Collectors.toList());
      }
      try (OutputStream os = Files.newOutputStream(ARCHIVE);
          ZipOutputStream zos = new ZipOutputStream(os)) {
        for (Path file : files) {
          // Entry names use "/" as the separator on every platform.
          String name =
              ARCHIVE_CONTENTS.relativize(file).toString().replace(File.separatorChar, '/');
          zos.putNextEntry(new ZipEntry(name));
          zos.write(Files.readAllBytes(file));
          zos.closeEntry();
        }
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    return ARCHIVE.toString();
  }

  @Parameters
  public static String[] getTestDirs() {
    return new String[] {"ajava-archive"};
  }
}
