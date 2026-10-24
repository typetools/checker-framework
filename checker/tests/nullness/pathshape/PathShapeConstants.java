// Tests the Path Shape Checker's classification of compile-time constant strings.

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.checkerframework.checker.nullness.qual.HasName;
import org.checkerframework.checker.nullness.qual.HasParent;

public class PathShapeConstants {

  static final String STATIC_CONSTANT = "a/b";

  void relative() {
    @HasName Path a = Path.of("a");
    // :: error: (assignment)
    @HasParent Path a2 = Path.of("a");
    @HasParent Path ab = Path.of("a/b");
    @HasParent Path abTrailing = Path.of("a/b/");
    @HasName Path dot = Path.of(".");
    // `a/..` has two names; it is not normalized.
    @HasParent Path aDotDot = Path.of("a/..");
    @HasParent Path absolute = Path.of("/a");
  }

  void rootsAndEmpty() {
    // :: error: (assignment)
    @HasName Path root = Path.of("/");
    // :: error: (assignment)
    @HasName Path empty = Path.of("");
    // `C:` is a name on Unix but a root on Windows.
    // :: error: (assignment)
    @HasName Path drive = Path.of("C:");
    // :: error: (assignment)
    @HasName Path driveRoot = Path.of("C:\\");
    // `\\server\share\` is a name on Unix but a root on Windows.
    // :: error: (assignment)
    @HasName Path unc = Path.of("\\\\server\\share\\");
  }

  void windows() {
    // `C:foo` is one name on Unix, and a root plus a name on Windows.
    @HasName Path driveRelative = Path.of("C:foo");
    // :: error: (assignment)
    @HasParent Path driveRelative2 = Path.of("C:foo");
    // A backslash separates names on Windows but not on Unix.
    @HasName Path backslash = Path.of("a\\b");
    // :: error: (assignment)
    @HasParent Path backslash2 = Path.of("a\\b");
  }

  void constantExpressions() {
    final String local = "a/b";
    @HasParent Path p1 = Path.of(local);
    @HasParent Path p2 = Path.of(STATIC_CONSTANT);
    @HasParent Path p3 = Path.of("a" + "/b");
    @HasParent Path p4 = Path.of(("a/b"));
    String notConstant = "a/b";
    // :: error: (assignment)
    @HasName Path p5 = Path.of(notConstant);
  }

  void varargs(String s, String[] more) {
    @HasParent Path p1 = Path.of("a", "b");
    @HasParent Path p2 = Paths.get("a", "b");
    // Empty strings are ignored when the strings are joined.
    @HasParent Path p3 = Path.of("a", "", "b");
    @HasName Path p4 = Path.of("", "a");
    @HasName Path p5 = Path.of("a", "");
    // :: error: (assignment)
    @HasName Path p6 = Path.of("", "");
    // :: error: (assignment)
    @HasName Path p7 = Path.of("/", "");
    @HasParent Path p8 = Path.of("/", "a");
    // :: error: (assignment)
    @HasName Path p9 = Path.of("a", s);
    // :: error: (assignment)
    @HasName Path p10 = Path.of("a", more);
    @HasName Path p11 = Paths.get("a");
  }

  void files() {
    @HasName File a = new File("a");
    // :: error: (assignment)
    @HasParent File a2 = new File("a");
    @HasParent File ab = new File("a/b");
    @HasParent File absolute = new File("/a");
    // :: error: (assignment)
    @HasName File root = new File("/");
    // :: error: (assignment)
    @HasName File empty = new File("");
    // :: error: (assignment)
    @HasName File drive = new File("C:");
    @HasName File driveRelative = new File("C:foo");
    // `\\server\share` has a parent as a File, but not as a Path.
    @HasName File unc = new File("\\\\server\\share");
    @HasParent File unc2 = new File("//server/share");
  }
}
