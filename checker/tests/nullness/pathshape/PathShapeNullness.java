// Tests how the Nullness Checker uses path shapes to refine the result of getParent() and related
// methods.

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import org.checkerframework.checker.nullness.qual.HasName;
import org.checkerframework.checker.nullness.qual.HasParent;
import org.checkerframework.checker.nullness.qual.NonNull;

public class PathShapeNullness {

  void pathGetParent(Path top, @HasName Path name, @HasParent Path parent) {
    @NonNull Path p1 = parent.getParent();
    @NonNull Path p2 = name.toAbsolutePath().getParent();
    @NonNull Path p3 = Path.of("a").toAbsolutePath().getParent();
    @NonNull Path p4 = Path.of("a/b").getParent();
    // :: error: (assignment)
    @NonNull Path p5 = name.getParent();
    // :: error: (assignment)
    @NonNull Path p6 = top.toAbsolutePath().getParent();
    // :: error: (assignment)
    @NonNull Path p7 = Path.of("/").getParent();
  }

  void pathGetFileName(Path top, @HasName Path name) {
    @NonNull Path p1 = name.getFileName();
    @NonNull Path p2 = Path.of("a").getFileName();
    // :: error: (assignment)
    @NonNull Path p3 = top.getFileName();
    // :: error: (assignment)
    @NonNull Path p4 = Path.of("/").getFileName();
  }

  void fileGetParent(File top, @HasName File name) {
    @NonNull File f1 = name.getAbsoluteFile().getParentFile();
    @NonNull String s1 = name.getAbsoluteFile().getParent();
    @NonNull File f2 = new File("a/b").getParentFile();
    // :: error: (assignment)
    @NonNull File f3 = name.getParentFile();
    // :: error: (assignment)
    @NonNull String s2 = top.getAbsoluteFile().getParent();
  }

  void regularFile(Path p, File f) {
    if (Files.isRegularFile(p)) {
      p.toAbsolutePath().getParent().toString();
      p.getFileName().toString();
    }
    if (f.isFile()) {
      f.getAbsoluteFile().getParentFile().toString();
    }
  }

  void flow(Path p) {
    Path abs = p.resolve("a").toAbsolutePath();
    abs.getParent().toString();
    abs = Path.of("/");
    // :: error: (dereference.of.nullable)
    abs.getParent().toString();
  }

  // The qualifiers apply only to Path and File.
  // :: error: (anno.on.irrelevant)
  @HasParent String s = "";
}
