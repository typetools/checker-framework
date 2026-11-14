package org.checkerframework.checker.nullness;

import java.io.File;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
 * expected results are checked against the running JDK: the Unix results when the tests run on
 * Unix, and the Windows results when the tests run on Windows. The expected Windows {@code File}
 * result covers JDKs both before and after JDK 25, which differ, so it is checked to be no more
 * specific than the running JDK's result.
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
    Assert.assertEquals("unixFile " + s, unixFile, PathShapeClassifier.unixShape(s));
    Assert.assertEquals("windowsFile " + s, windowsFile, PathShapeClassifier.windowsFile(s));
    Assert.assertEquals("classifyPath " + s, path, PathShapeClassifier.classifyPath(s));
    Assert.assertEquals("classifyFile " + s, file, PathShapeClassifier.classifyFile(s));
    if (File.separatorChar == '/') {
      Assert.assertEquals("actual Unix Path " + s, unixPath, actualPathShape(s));
      Assert.assertEquals("actual Unix File " + s, unixFile, actualFileShape(s));
    } else if (File.separatorChar == '\\') {
      Assert.assertEquals("actual Windows Path " + s, windowsPath, actualPathShape(s));
      PathShape actualFile = actualFileShape(s);
      // A JDK either strips a long-path prefix (JDK 25 and later) or does not.
      PathShape beforeJdk25 = PathShapeClassifier.windowsFileShape(s);
      PathShape sinceJdk25 =
          PathShapeClassifier.windowsFileShape(PathShapeClassifier.stripLongPathPrefix(s));
      Assert.assertTrue(
          "actual Windows File " + s + " is " + actualFile,
          actualFile == beforeJdk25 || actualFile == sinceJdk25);
      Assert.assertEquals("sound Windows File " + s, windowsFile, windowsFile.lub(actualFile));
    }
  }

  /**
   * Checks the path shape of {@code Path.of(first, more...)}, under both the classifier and the
   * running JDK.
   *
   * @param expected the expected result of {@link PathShapeClassifier#classifyPath(List)}
   * @param first the first argument to {@code Path.of}
   * @param more the remaining arguments to {@code Path.of}
   */
  private static void checkJoin(PathShape expected, String first, String... more) {
    List<String> strings = new ArrayList<>();
    strings.add(first);
    strings.addAll(Arrays.asList(more));
    String desc = strings.toString();
    Assert.assertEquals(
        "classifyPath " + desc, expected, PathShapeClassifier.classifyPath(strings));
    @Nullable PathShape actual;
    try {
      actual = actualShape(Path.of(first, more));
    } catch (InvalidPathException e) {
      actual = null;
    }
    if (File.separatorChar == '/') {
      Assert.assertEquals(
          "actual Unix Path " + desc,
          PathShapeClassifier.unixPath(PathShapeClassifier.join(strings, '/')),
          actual);
    } else if (File.separatorChar == '\\') {
      Assert.assertEquals(
          "actual Windows Path " + desc,
          PathShapeClassifier.windowsPath(PathShapeClassifier.join(strings, '\\')),
          actual);
    }
  }

  /**
   * Returns the shape of {@code Path.of(s)} on the current platform.
   *
   * @param s a path string
   * @return the shape of {@code Path.of(s)}, or null if {@code s} is not a valid path
   */
  private static @Nullable PathShape actualPathShape(String s) {
    try {
      return actualShape(Path.of(s));
    } catch (InvalidPathException e) {
      return null;
    }
  }

  /**
   * Returns the shape of the given path.
   *
   * @param p a path
   * @return the shape of {@code p}
   */
  private static PathShape actualShape(Path p) {
    Path fileName = p.getFileName();
    return PathShape.of(fileName != null && !fileName.toString().isEmpty(), p.getParent() != null);
  }

  /**
   * Returns the shape of {@code new File(s)} on the current platform.
   *
   * @param s a path string
   * @return the shape of {@code new File(s)}
   */
  private static PathShape actualFileShape(String s) {
    File f = new File(s);
    return PathShape.of(!f.getPath().isEmpty() && !f.getName().isEmpty(), f.getParent() != null);
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
    // A Path requires an absolute path after the long-path prefix.  A File on JDK 25 or later
    // strips the prefix, leaving the relative path "foo".
    check("\\\\?\\foo", HAS_NAME, null, HAS_NAME, HAS_NAME, HAS_NAME, HAS_NAME);
    // A File on JDK 25 or later strips the prefix, leaving a root.
    check("\\\\?\\C:\\", HAS_NAME, UNKNOWN, HAS_NAME, UNKNOWN, UNKNOWN, UNKNOWN);
    check("\\\\?\\C:", HAS_NAME, null, HAS_NAME, UNKNOWN, HAS_NAME, UNKNOWN);
    check("\\\\?\\UNC", HAS_NAME, null, HAS_NAME, UNKNOWN, HAS_NAME, UNKNOWN);
    check("\\\\?\\UNC\\", HAS_NAME, null, HAS_NAME, UNKNOWN, HAS_NAME, UNKNOWN);
    check("\\\\?\\\\", HAS_NAME, null, HAS_NAME, UNKNOWN, HAS_NAME, UNKNOWN);
  }

  @Test
  public void testJoin() {
    checkJoin(HAS_NAME, "a");
    checkJoin(HAS_PARENT, "a", "b");
    checkJoin(HAS_PARENT, "a", "", "b");
    checkJoin(HAS_NAME, "", "a");
    checkJoin(UNKNOWN, "", "");
    checkJoin(HAS_PARENT, "/", "a");
    checkJoin(HAS_PARENT, "a/", "/b");
    // Windows joins the strings with a backslash, which forms the long-path prefix "\\?\", so
    // the result is the root "C:\".  On Unix, the result has two names.
    checkJoin(UNKNOWN, "\\\\?", "C:\\");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testJoinEmpty() {
    PathShapeClassifier.classifyPath(new ArrayList<>());
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
