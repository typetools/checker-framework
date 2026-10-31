package org.checkerframework.checker.nullness;

import java.util.List;
import org.checkerframework.checker.nullness.qual.HasName;
import org.checkerframework.checker.nullness.qual.HasParent;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Determines the path shape of a path string: whether the {@link java.nio.file.Path} or {@link
 * java.io.File} that is constructed from the string has a name and has a parent. The result is
 * correct under both Unix and Windows rules, because it is the weaker of the Unix result and the
 * Windows result.
 *
 * <p>The Windows rules are derived from the JDK source code: {@code sun.nio.fs.WindowsPathParser}
 * and {@code sun.nio.fs.WindowsPath} for {@code Path}, and {@code java.io.WinNTFileSystem} and
 * {@code java.io.File} for {@code File}. The two differ; for example, {@code \\server\share} is a
 * root as a {@code Path}, but it has a parent ({@code \\server}) as a {@code File}.
 *
 * @checker_framework.manual #path-shape-checker Path Shape Checker
 */
public final class PathShapeClassifier {

  /** Do not instantiate. */
  private PathShapeClassifier() {
    throw new Error("Do not instantiate");
  }

  /** The shape of a path, from least to most specific. */
  public enum PathShape {
    /** The path might be a root or the empty path. */
    UNKNOWN,
    /** The path has a name. See {@link HasName}. */
    HAS_NAME,
    /** The path has a parent and a name. See {@link HasParent}. */
    HAS_PARENT;

    /**
     * Returns the less specific of this and {@code other}.
     *
     * @param other another path shape
     * @return the less specific of this and {@code other}
     */
    public PathShape lub(PathShape other) {
      return this.compareTo(other) <= 0 ? this : other;
    }
  }

  /**
   * Returns the shape of {@code Path.of(s)}.
   *
   * @param s a path string
   * @return the shape of {@code Path.of(s)} under both Unix and Windows rules
   */
  public static PathShape classifyPath(String s) {
    return weaker(unixPath(s), windowsPath(s));
  }

  /**
   * Returns the shape of {@code Path.of(first, more...)}.
   *
   * @param strings the arguments to {@code Path.of}: {@code first}, followed by the elements of
   *     {@code more}
   * @return the shape of {@code Path.of(first, more...)} under both Unix and Windows rules
   */
  public static PathShape classifyPath(List<String> strings) {
    // Each platform joins the strings with its own separator.  The Windows result can depend on
    // the separator; for example, Windows recognizes the prefix "\\?\" only with backslashes.
    return weaker(unixPath(join(strings, '/')), windowsPath(join(strings, '\\')));
  }

  /**
   * Joins the arguments of {@code Path.of(first, more...)} into a path string, as {@code
   * FileSystem.getPath} does: it omits empty strings in {@code more}.
   *
   * @param strings {@code first}, followed by the elements of {@code more}
   * @param separator the platform's separator
   * @return the path string
   */
  private static String join(List<String> strings, char separator) {
    StringBuilder sb = new StringBuilder(strings.get(0));
    for (String segment : strings.subList(1, strings.size())) {
      if (!segment.isEmpty()) {
        if (sb.length() > 0) {
          sb.append(separator);
        }
        sb.append(segment);
      }
    }
    return sb.toString();
  }

  /**
   * Returns the shape of {@code new File(s)}.
   *
   * @param s a path string
   * @return the shape of {@code new File(s)} under both Unix and Windows rules
   */
  public static PathShape classifyFile(String s) {
    return weaker(unixShape(s), windowsFile(s));
  }

  /**
   * Returns the weaker of two per-platform results. A null argument means that the string is not a
   * valid path on that platform, so the constructor throws an exception and the platform imposes no
   * constraint.
   *
   * @param unix the Unix result, or null if the string is invalid on Unix
   * @param windows the Windows result, or null if the string is invalid on Windows
   * @return the weaker of the two results
   */
  private static PathShape weaker(@Nullable PathShape unix, @Nullable PathShape windows) {
    if (unix == null) {
      return windows == null ? PathShape.UNKNOWN : windows;
    } else if (windows == null) {
      return unix;
    } else {
      return unix.lub(windows);
    }
  }

  /**
   * Returns the shape that a path has, given its root and its number of name elements.
   *
   * @param hasRoot true if the path has a root component
   * @param nameCount the number of name elements in the path, not counting the root; each of them
   *     is non-empty
   * @return the shape of the path
   */
  private static PathShape shapeFromNames(boolean hasRoot, int nameCount) {
    if (nameCount == 0) {
      return PathShape.UNKNOWN;
    } else if (hasRoot || nameCount >= 2) {
      return PathShape.HAS_PARENT;
    } else {
      return PathShape.HAS_NAME;
    }
  }

  // ///////////////////////////////////////////////////////////////////////////
  // Unix
  //

  /**
   * Returns the shape of {@code Path.of(s)} on Unix.
   *
   * @param s a path string
   * @return the shape of {@code Path.of(s)} on Unix, or null if {@code s} is not a valid Unix path
   */
  /*package-private*/ static @Nullable PathShape unixPath(String s) {
    // UnixPath.normalizeAndCheck forbids the NUL character.
    if (s.indexOf('\u0000') != -1) {
      return null;
    }
    return unixShape(s);
  }

  /**
   * Returns the shape of a Unix path. It is the shape of {@code new File(s)} on Unix, and of {@code
   * Path.of(s)} if {@code s} is a valid Unix path. Unix path normalization, for both {@code Path}
   * and {@code File}, removes redundant and trailing slashes.
   *
   * @param s a path string
   * @return the shape of the Unix path
   */
  /*package-private*/ static PathShape unixShape(String s) {
    boolean hasRoot = s.startsWith("/");
    int nameCount = 0;
    for (String name : s.split("/", -1)) {
      if (!name.isEmpty()) {
        nameCount++;
      }
    }
    return shapeFromNames(hasRoot, nameCount);
  }

  // ///////////////////////////////////////////////////////////////////////////
  // Windows Path:  sun.nio.fs.WindowsPathParser and sun.nio.fs.WindowsPath
  //

  /** Characters that may not appear in a Windows path, other than control characters. */
  private static final String WINDOWS_RESERVED_CHARS = "<>:\"|?*";

  /**
   * Returns the shape of {@code Path.of(s)} on Windows. This follows {@code
   * WindowsPathParser.parse}.
   *
   * @param s a path string
   * @return the shape of {@code Path.of(s)} on Windows, or null if {@code s} is not a valid Windows
   *     path
   */
  /*package-private*/ static @Nullable PathShape windowsPath(String s) {
    String input = s;

    // A long-path prefix:  "\\?\" or "\\?\UNC\".
    boolean expectUnc = false;
    boolean expectAbsolute = false;
    if (input.startsWith("\\\\?\\")) {
      if (input.startsWith("UNC\\", 4)) {
        expectUnc = true;
        input = "\\\\" + input.substring(8);
      } else {
        expectAbsolute = true;
        input = input.substring(4);
      }
    }

    boolean hasRoot;
    boolean isUnc = false;
    boolean isAbsolute = false;
    int len = input.length();
    int off = 0;
    if (len > 1 && isWindowsSlash(input.charAt(0)) && isWindowsSlash(input.charAt(1))) {
      // UNC:  "\\server\share\".
      isUnc = true;
      off = nextNonSlash(input, 2, len);
      int next = nextSlash(input, off, len);
      if (off == next || hasInvalidWindowsPathChar(input, off, next)) {
        // The UNC path is missing the host name, or the host name is invalid.
        return null;
      }
      off = nextNonSlash(input, next, len);
      next = nextSlash(input, off, len);
      if (off == next || hasInvalidWindowsPathChar(input, off, next)) {
        // The UNC path is missing the share name, or the share name is invalid.
        return null;
      }
      off = next;
      hasRoot = true;
    } else if (len > 1 && isAsciiLetter(input.charAt(0)) && input.charAt(1) == ':') {
      // "C:\" (absolute) or "C:" (drive-relative).
      if (len > 2 && isWindowsSlash(input.charAt(2))) {
        isAbsolute = true;
        off = 3;
      } else {
        off = 2;
      }
      hasRoot = true;
    } else {
      // "\" (directory-relative) or no root (relative).
      hasRoot = len > 0 && isWindowsSlash(input.charAt(0));
    }

    if ((expectUnc && !isUnc) || (expectAbsolute && !isAbsolute)) {
      return null;
    }

    // Count the names in the remainder, as WindowsPathParser.normalize does.
    int nameCount = 0;
    off = nextNonSlash(input, off, len);
    while (off < len) {
      int next = nextSlash(input, off, len);
      if (hasInvalidWindowsPathChar(input, off, next)) {
        return null;
      }
      if (input.charAt(next - 1) == ' ') {
        // A name may not end with a space.
        return null;
      }
      nameCount++;
      off = nextNonSlash(input, next, len);
    }

    return shapeFromNames(hasRoot, nameCount);
  }

  /**
   * Returns true if the character is a Windows path separator.
   *
   * @param c a character
   * @return true if the character is a Windows path separator
   */
  private static boolean isWindowsSlash(char c) {
    return c == '\\' || c == '/';
  }

  /**
   * Returns true if the character is an ASCII letter, which can be a Windows drive letter.
   *
   * @param c a character
   * @return true if the character is an ASCII letter
   */
  private static boolean isAsciiLetter(char c) {
    return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
  }

  /**
   * Returns true if the given part of the string contains a character that may not appear in a name
   * in a Windows {@code Path}.
   *
   * @param s a string
   * @param from the index of the first character to examine
   * @param to the index after the last character to examine
   * @return true if {@code s.substring(from, to)} contains a character that may not appear in a
   *     Windows {@code Path} name
   */
  private static boolean hasInvalidWindowsPathChar(String s, int from, int to) {
    for (int i = from; i < to; i++) {
      char c = s.charAt(i);
      if (c < ' ' || WINDOWS_RESERVED_CHARS.indexOf(c) != -1) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns the index of the first character at or after {@code off} that is not a Windows path
   * separator, or {@code len} if there is none.
   *
   * @param s a string
   * @param off the index at which to start searching
   * @param len the length of {@code s}
   * @return the index of the next non-separator character, or {@code len}
   */
  private static int nextNonSlash(String s, int off, int len) {
    while (off < len && isWindowsSlash(s.charAt(off))) {
      off++;
    }
    return off;
  }

  /**
   * Returns the index of the first Windows path separator at or after {@code off}, or {@code len}
   * if there is none.
   *
   * @param s a string
   * @param off the index at which to start searching
   * @param len the length of {@code s}
   * @return the index of the next separator, or {@code len}
   */
  private static int nextSlash(String s, int off, int len) {
    while (off < len && !isWindowsSlash(s.charAt(off))) {
      off++;
    }
    return off;
  }

  // ///////////////////////////////////////////////////////////////////////////
  // Windows File:  java.io.WinNTFileSystem and java.io.File
  //

  /**
   * Returns the shape of {@code new File(s)} on Windows. This follows {@code File.getName} and
   * {@code File.getParent}.
   *
   * @param s a path string
   * @return the shape of {@code new File(s)} on Windows
   */
  /*package-private*/ static PathShape windowsFile(String s) {
    String path = windowsFileNormalize(s);
    int prefixLength = windowsFilePrefixLength(path);
    int index = path.lastIndexOf('\\');
    boolean hasName;
    boolean hasParent;
    if (index < prefixLength) {
      hasName = path.length() > prefixLength;
      hasParent = prefixLength > 0 && path.length() > prefixLength;
    } else {
      hasName = index + 1 < path.length();
      hasParent = true;
    }
    if (!hasName) {
      return PathShape.UNKNOWN;
    } else if (hasParent) {
      return PathShape.HAS_PARENT;
    } else {
      return PathShape.HAS_NAME;
    }
  }

  /**
   * Normalizes a Windows {@code File} path string, as {@code WinNTFileSystem.normalize} does.
   *
   * @param path a path string
   * @return the normalized path string
   */
  /*package-private*/ static String windowsFileNormalize(String path) {
    int n = path.length();
    char prev = 0;
    for (int i = 0; i < n; i++) {
      char c = path.charAt(i);
      if (c == '/') {
        return windowsFileNormalize(path, n, (prev == '\\') ? i - 1 : i);
      }
      if ((c == '\\') && (prev == '\\') && (i > 1)) {
        return windowsFileNormalize(path, n, i - 1);
      }
      if ((c == ':') && (i > 1)) {
        return windowsFileNormalize(path, n, 0);
      }
      prev = c;
    }
    if (prev == '\\') {
      return windowsFileNormalize(path, n, n - 1);
    }
    return path;
  }

  /**
   * Normalizes a Windows {@code File} path string, starting at the given offset; everything before
   * the offset is already normal. This follows {@code WinNTFileSystem.normalize(String, int, int)}.
   *
   * @param path a path string
   * @param len the length of {@code path}
   * @param off the offset at which to start normalizing
   * @return the normalized path string
   */
  private static String windowsFileNormalize(String path, int len, int off) {
    if (len == 0) {
      return path;
    }
    if (off < 3) {
      // Avoid fencepost cases with UNC pathnames.
      off = 0;
    }
    int src;
    StringBuilder sb = new StringBuilder(len);

    if (off == 0) {
      src = windowsFileNormalizePrefix(path, len, sb);
    } else {
      src = off;
      sb.append(path, 0, off);
    }

    // Remove redundant slashes from the remainder of the path, forcing all slashes into '\'.
    while (src < len) {
      char c = path.charAt(src++);
      if (isWindowsSlash(c)) {
        while ((src < len) && isWindowsSlash(path.charAt(src))) {
          src++;
        }
        if (src == len) {
          // Check for a trailing separator.
          int sn = sb.length();
          if ((sn == 2) && (sb.charAt(1) == ':')) {
            // "z:\"
            sb.append('\\');
          } else if (sn == 0) {
            // "\"
            sb.append('\\');
          } else if ((sn == 1) && isWindowsSlash(sb.charAt(0))) {
            // "\\"
            sb.append('\\');
          }
          break;
        } else {
          sb.append('\\');
        }
      } else {
        sb.append(c);
      }
    }

    return sb.toString();
  }

  /**
   * Normalizes the prefix of a Windows {@code File} path string, as {@code
   * WinNTFileSystem.normalizePrefix} does.
   *
   * @param path a path string
   * @param len the length of {@code path}
   * @param sb where to write the normalized prefix
   * @return the index in {@code path} at which normalization should continue
   */
  private static int windowsFileNormalizePrefix(String path, int len, StringBuilder sb) {
    int src = 0;
    while ((src < len) && isWindowsSlash(path.charAt(src))) {
      src++;
    }
    if ((len - src >= 2) && isAsciiLetter(path.charAt(src)) && path.charAt(src + 1) == ':') {
      // Remove leading slashes if followed by a drive specifier.
      sb.append(path.charAt(src));
      sb.append(':');
      src += 2;
    } else {
      src = 0;
      if ((len >= 2) && isWindowsSlash(path.charAt(0)) && isWindowsSlash(path.charAt(1))) {
        // UNC pathname:  retain the first slash, and leave src pointing at the second slash so that
        // further slashes are collapsed into it.
        src = 1;
        sb.append('\\');
      }
    }
    return src;
  }

  /**
   * Returns the length of the prefix of a normalized Windows {@code File} path string, as {@code
   * WinNTFileSystem.prefixLength} does.
   *
   * @param path a normalized path string
   * @return the length of the prefix: 0 for a relative path, 1 for a drive-relative path such as
   *     {@code \foo}, 2 for a UNC path such as {@code \\foo} or a directory-relative path such as
   *     {@code z:foo}, and 3 for an absolute path such as {@code z:\foo}
   */
  private static int windowsFilePrefixLength(String path) {
    int n = path.length();
    if (n == 0) {
      return 0;
    }
    char c0 = path.charAt(0);
    char c1 = (n > 1) ? path.charAt(1) : 0;
    if (c0 == '\\') {
      return (c1 == '\\') ? 2 : 1;
    }
    if (isAsciiLetter(c0) && (c1 == ':')) {
      return ((n > 2) && (path.charAt(2) == '\\')) ? 3 : 2;
    }
    return 0;
  }
}
