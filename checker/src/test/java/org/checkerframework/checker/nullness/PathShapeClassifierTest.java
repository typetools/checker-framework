package org.checkerframework.checker.nullness;

import java.io.File;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Arrays;
import org.checkerframework.checker.nullness.PathShapeClassifier.PathShape;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests {@link PathShapeClassifier}.
 *
 * <p>The project does not run tests on Windows, so the expected Windows results are derived from
 * the JDK source code: {@code sun.nio.fs.WindowsPathParser} and {@code sun.nio.fs.WindowsPath} for
 * {@code Path}, and {@code java.io.WinNTFileSystem} and {@code java.io.File} for {@code File}. The
 * expected Unix results are checked against the running JDK when the tests run on Unix.
 */
public class PathShapeClassifierTest {

  /** Abbreviation for {@link PathShape#HAS_NAME}. */
  private static final PathShape HAS_NAME = PathShape.HAS_NAME;

  /** Abbreviation for {@link PathShape#HAS_PARENT}. */
  private static final PathShape HAS_PARENT = PathShape.HAS_PARENT;

  /** Abbreviation for {@link PathShape#UNKNOWN}. */
  private static final PathShape UNKNOWN = PathShape.UNKNOWN;

  /**
   * Checks the path shape of a string under each set of rules.
   *
   * @param s a path string
   * @param unixPath the expected shape of {@code Path.of(s)} on Unix, or null if it throws
   * @param windowsPath the expected shape of {@code Path.of(s)} on Windows, or null if it throws
   * @param unixFile the expected shape of {@code new File(s)} on Unix
   * @param windowsFile the expected shape of {@code new File(s)} on Windows
   * @param path the expected result of {@link PathShapeClassifier#classifyPath}
   * @param file the expected result of {@link PathShapeClassifier#classifyFile}
   */
  private static void check(
      String s,
      @Nullable PathShape unixPath,
      @Nullable PathShape windowsPath,
      PathShape unixFile,
      PathShape windowsFile,
      PathShape path,
      PathShape file) {
    Assert.assertEquals("unixPath " + s, unixPath, PathShapeClassifier.unixPath(s));
    Assert.assertEquals("windowsPath " + s, windowsPath, PathShapeClassifier.windowsPath(s));
    Assert.assertEquals("unixFile " + s, unixFile, PathShapeClassifier.unixFile(s));
    Assert.assertEquals("windowsFile " + s, windowsFile, PathShapeClassifier.windowsFile(s));
    Assert.assertEquals("classifyPath " + s, path, PathShapeClassifier.classifyPath(s));
    Assert.assertEquals("classifyFile " + s, file, PathShapeClassifier.classifyFile(s));
    if (File.separatorChar == '/') {
      Assert.assertEquals("actual Unix Path " + s, unixPath, actualPathShape(s));
      Assert.assertEquals("actual Unix File " + s, unixFile, actualFileShape(s));
    }
  }

  /**
   * Returns the shape of {@code Path.of(s)} on the current platform.
   *
   * @param s a path string
   * @return the shape of {@code Path.of(s)}, or null if {@code s} is not a valid path
   */
  private static @Nullable PathShape actualPathShape(String s) {
    Path p;
    try {
      p = Path.of(s);
    } catch (InvalidPathException e) {
      return null;
    }
    Path fileName = p.getFileName();
    return shape(fileName != null && !fileName.toString().isEmpty(), p.getParent() != null);
  }

  /**
   * Returns the shape of {@code new File(s)} on the current platform.
   *
   * @param s a path string
   * @return the shape of {@code new File(s)}
   */
  private static PathShape actualFileShape(String s) {
    File f = new File(s);
    return shape(!f.getPath().isEmpty() && !f.getName().isEmpty(), f.getParent() != null);
  }

  /**
   * Returns the path shape with the given properties.
   *
   * @param hasName true if the path has a non-empty name
   * @param hasParent true if the path has a parent
   * @return the path shape with the given properties
   */
  private static PathShape shape(boolean hasName, boolean hasParent) {
    return !hasName ? UNKNOWN : hasParent ? HAS_PARENT : HAS_NAME;
  }

  @Test
  public void testRelative() {
    check("a", HAS_NAME, HAS_NAME, HAS_NAME, HAS_NAME, HAS_NAME, HAS_NAME);
    check("a/", HAS_NAME, HAS_NAME, HAS_NAME, HAS_NAME, HAS_NAME, HAS_NAME);
    check(".", HAS_NAME, HAS_NAME, HAS_NAME, HAS_NAME, HAS_NAME, HAS_NAME);
    check("..", HAS_NAME, HAS_NAME, HAS_NAME, HAS_NAME, HAS_NAME, HAS_NAME);
    check("a/b", HAS_PARENT, HAS_PARENT, HAS_PARENT, HAS_PARENT, HAS_PARENT, HAS_PARENT);
    check("a/..", HAS_PARENT, HAS_PARENT, HAS_PARENT, HAS_PARENT, HAS_PARENT, HAS_PARENT);
    // A backslash is a separator on Windows, but not on Unix.
    check("a\\b", HAS_NAME, HAS_PARENT, HAS_NAME, HAS_PARENT, HAS_NAME, HAS_NAME);
  }

  @Test
  public void testEmptyAndRoots() {
    check("", UNKNOWN, UNKNOWN, UNKNOWN, UNKNOWN, UNKNOWN, UNKNOWN);
    check("/", UNKNOWN, UNKNOWN, UNKNOWN, UNKNOWN, UNKNOWN, UNKNOWN);
    check("//", UNKNOWN, null, UNKNOWN, UNKNOWN, UNKNOWN, UNKNOWN);
    check("\\", HAS_NAME, UNKNOWN, HAS_NAME, UNKNOWN, UNKNOWN, UNKNOWN);
    check("/a", HAS_PARENT, HAS_PARENT, HAS_PARENT, HAS_PARENT, HAS_PARENT, HAS_PARENT);
    check("\\foo", HAS_NAME, HAS_PARENT, HAS_NAME, HAS_PARENT, HAS_NAME, HAS_NAME);
  }

  @Test
  public void testDrives() {
    check("C:", HAS_NAME, UNKNOWN, HAS_NAME, UNKNOWN, UNKNOWN, UNKNOWN);
    check("C:\\", HAS_NAME, UNKNOWN, HAS_NAME, UNKNOWN, UNKNOWN, UNKNOWN);
    check("C:/", HAS_NAME, UNKNOWN, HAS_NAME, UNKNOWN, UNKNOWN, UNKNOWN);
    check("C:foo", HAS_NAME, HAS_PARENT, HAS_NAME, HAS_PARENT, HAS_NAME, HAS_NAME);
    check("C:\\foo", HAS_NAME, HAS_PARENT, HAS_NAME, HAS_PARENT, HAS_NAME, HAS_NAME);
    check("a:b", HAS_NAME, HAS_PARENT, HAS_NAME, HAS_PARENT, HAS_NAME, HAS_NAME);
    // A Windows File removes slashes before a drive letter; a Windows Path forbids the colon.
    check("/C:/x", HAS_PARENT, null, HAS_PARENT, HAS_PARENT, HAS_PARENT, HAS_PARENT);
  }

  @Test
  public void testUnc() {
    check("\\\\", HAS_NAME, null, HAS_NAME, UNKNOWN, HAS_NAME, UNKNOWN);
    check("\\\\server", HAS_NAME, null, HAS_NAME, HAS_PARENT, HAS_NAME, HAS_NAME);
    // A Path treats "\\server\share" as a root, but a File treats it as having a parent.
    check("\\\\server\\share", HAS_NAME, UNKNOWN, HAS_NAME, HAS_PARENT, UNKNOWN, HAS_NAME);
    check("\\\\server\\share\\", HAS_NAME, UNKNOWN, HAS_NAME, HAS_PARENT, UNKNOWN, HAS_NAME);
    check("\\\\server\\share\\a", HAS_NAME, HAS_PARENT, HAS_NAME, HAS_PARENT, HAS_NAME, HAS_NAME);
    check("//server/share", HAS_PARENT, UNKNOWN, HAS_PARENT, HAS_PARENT, UNKNOWN, HAS_PARENT);
    check(
        "//server/share/a", HAS_PARENT, HAS_PARENT, HAS_PARENT, HAS_PARENT, HAS_PARENT, HAS_PARENT);
  }

  @Test
  public void testLongPathPrefix() {
    check("\\\\?\\C:\\foo", HAS_NAME, HAS_PARENT, HAS_NAME, HAS_PARENT, HAS_NAME, HAS_NAME);
    check(
        "\\\\?\\UNC\\server\\share\\a",
        HAS_NAME,
        HAS_PARENT,
        HAS_NAME,
        HAS_PARENT,
        HAS_NAME,
        HAS_NAME);
    // The long-path prefix requires an absolute path.
    check("\\\\?\\foo", HAS_NAME, null, HAS_NAME, HAS_PARENT, HAS_NAME, HAS_NAME);
  }

  @Test
  public void testJoin() {
    Assert.assertEquals(HAS_PARENT, PathShapeClassifier.classifyPath(Arrays.asList("a", "b")));
    Assert.assertEquals(HAS_PARENT, PathShapeClassifier.classifyPath(Arrays.asList("a", "", "b")));
    Assert.assertEquals(HAS_NAME, PathShapeClassifier.classifyPath(Arrays.asList("", "a")));
    Assert.assertEquals(UNKNOWN, PathShapeClassifier.classifyPath(Arrays.asList("", "")));
    // Windows joins the strings with a backslash, which forms the long-path prefix "\\?\", so
    // the result is the root "C:\".  On Unix, the result has two names.
    Assert.assertEquals(UNKNOWN, PathShapeClassifier.classifyPath(Arrays.asList("\\\\?", "C:\\")));
  }

  @Test
  public void testInvalid() {
    // A Windows Path forbids some characters and a trailing space; a File does not.
    check("foo:bar", HAS_NAME, null, HAS_NAME, HAS_NAME, HAS_NAME, HAS_NAME);
    check("a?b", HAS_NAME, null, HAS_NAME, HAS_NAME, HAS_NAME, HAS_NAME);
    check("a ", HAS_NAME, null, HAS_NAME, HAS_NAME, HAS_NAME, HAS_NAME);
    check("\\\\a?b\\share\\c", HAS_NAME, null, HAS_NAME, HAS_PARENT, HAS_NAME, HAS_NAME);
    // No Path may contain the NUL character.
    check("a\u0000b", null, null, HAS_NAME, HAS_NAME, UNKNOWN, HAS_NAME);
  }
}
