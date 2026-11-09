// Tests the edge cases that the Path Shape Checker's rules must handle.

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.stream.Stream;
import org.checkerframework.checker.nullness.qual.HasName;
import org.checkerframework.checker.nullness.qual.HasParent;
import org.checkerframework.checker.nullness.qual.Nullable;

public class PathShapeEdgeCases {

  void emptyPath() {
    // Path.of("") has 1 name, its getFileName() is "", and its toAbsolutePath() might be "/".
    // :: error: (assignment)
    @HasName Path p1 = Path.of("");
    // :: error: (dereference.of.nullable)
    Path.of("").toAbsolutePath().getParent().toString();
  }

  void listingOfEmptyPath() throws IOException {
    // The entries of the empty path have no parent.
    // :: error: (assignment)
    Stream<@HasParent Path> s1 = Files.list(Path.of(""));
    Stream<@HasName Path> s2 = Files.list(Path.of(""));
    // :: error: (assignment)
    @HasParent File @Nullable [] fs1 = new File("").listFiles();
    // :: error: (assignment)
    @HasParent Path t1 = Files.createTempFile(Path.of(""), "p", "s");
    @HasName Path t2 = Files.createTempFile(Path.of(""), "p", "s");
  }

  void tempFileInTmpdir() throws IOException {
    // The java.io.tmpdir system property might be empty, so the result might have no parent.
    // :: error: (dereference.of.nullable)
    Files.createTempFile("p", "s").getParent().toString();
    Files.createTempFile("p", "s").toAbsolutePath().getParent().toString();
  }

  void walkOfEmptyPath() throws IOException {
    // The first element of Files.walk(Path.of("")) is "".
    // :: error: (assignment)
    Stream<@HasName Path> s = Files.walk(Path.of(""));
  }

  void normalizeAndRealPath() throws IOException {
    // Path.of("a/..").normalize() is "".
    @HasParent Path p1 = Path.of("a/..");
    // :: error: (assignment)
    @HasName Path p2 = Path.of("a/..").normalize();
    // :: error: (dereference.of.nullable)
    Path.of("a/..").normalize().toAbsolutePath().getParent().toString();
    // toRealPath() and getCanonicalFile() might return "/", for example via a symbolic link.
    // :: error: (assignment)
    @HasName Path p3 = Path.of("sub/..").toRealPath();
    // :: error: (assignment)
    @HasName File f1 = new File("sub/..").getCanonicalFile();
  }

  void dotDot() {
    // toAbsolutePath() does not normalize, so "..".toAbsolutePath() is "/cwd/..".
    Path.of("..").toAbsolutePath().getParent().toString();
  }

  void nullArguments(@HasParent File parent) {
    // Null belongs to every qualifier.  `new File(null, child)` is `new File(child)`.
    @HasParent File f1 = new File((File) null, "a/b");
    @HasName File f2 = new File((String) null, "a");
    // :: error: (assignment)
    @HasParent File f3 = new File((File) null, "a");
    @HasParent Path p1 = null;
  }

  void emptyParent() {
    // `new File("", "x")` is "/x", but the rule ignores the parent.
    @HasName File f1 = new File("", "x");
    @HasName File f2 = new File(new File(""), "x");
  }

  void walkFileTree(Path start) throws IOException {
    Files.walkFileTree(
        start,
        new SimpleFileVisitor<Path>() {
          @Override
          public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
            // The parameter is top.
            // :: error: (assignment)
            @HasName Path p = file;
            return FileVisitResult.CONTINUE;
          }
        });
  }
}
