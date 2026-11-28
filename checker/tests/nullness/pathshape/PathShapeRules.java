// Tests the Path Shape Checker's type rules for methods and constructors.

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.checkerframework.checker.nullness.qual.HasName;
import org.checkerframework.checker.nullness.qual.HasParent;

public class PathShapeRules {

  void toAbsolutePath(Path top, @HasName Path name, @HasParent Path parent) {
    // :: error: (assignment)
    @HasName Path p1 = top.toAbsolutePath();
    @HasParent Path p2 = name.toAbsolutePath();
    @HasParent Path p3 = parent.toAbsolutePath();
  }

  void getAbsoluteFile(File top, @HasName File name) {
    // :: error: (assignment)
    @HasName File f1 = top.getAbsoluteFile();
    @HasParent File f2 = name.getAbsoluteFile();
  }

  void normalize(@HasParent Path p, @HasParent File f) throws IOException {
    // :: error: (assignment)
    @HasName Path p1 = p.normalize();
    // :: error: (assignment)
    @HasName Path p2 = p.toRealPath();
    // :: error: (assignment)
    @HasName File f1 = f.getCanonicalFile();
  }

  void resolve(Path top, @HasName Path name, @HasParent Path parent) {
    @HasParent Path p1 = top.resolve(parent);
    @HasParent Path p2 = name.resolve(name);
    @HasName Path p3 = top.resolve(name);
    // :: error: (assignment)
    @HasParent Path p4 = top.resolve(name);
    // :: error: (assignment)
    @HasName Path p5 = parent.resolve(top);
    @HasParent Path p6 = name.resolve("b");
    @HasName Path p7 = top.resolve("b");
    // :: error: (assignment)
    @HasParent Path p8 = top.resolve("b");
    @HasParent Path p9 = top.resolve("b/c");
    // :: error: (assignment)
    @HasName Path p10 = name.resolve("C:");
  }

  void resolveSibling(Path top, @HasName Path name, @HasParent Path parent) {
    @HasParent Path p1 = top.resolveSibling(parent);
    @HasParent Path p2 = parent.resolveSibling(name);
    @HasName Path p3 = name.resolveSibling(name);
    // :: error: (assignment)
    @HasParent Path p4 = name.resolveSibling(name);
    // :: error: (assignment)
    @HasName Path p5 = parent.resolveSibling(top);
    @HasParent Path p6 = parent.resolveSibling("b");
    @HasParent Path p7 = top.resolveSibling("a/b");
  }

  void names(Path top, @HasName Path name, @HasParent Path parent) {
    @HasName Path p1 = name.getFileName();
    @HasName Path p2 = parent.getFileName();
    // A @HasParent path's file name has no parent.
    // :: error: (assignment)
    @HasParent Path p3 = parent.getFileName();
    // :: error: (assignment)
    @HasName Path p4 = top.getFileName();
    @HasName Path p5 = name.getName(0);
    @HasName Path p6 = parent.subpath(0, 1);
    // :: error: (assignment)
    @HasName Path p7 = top.subpath(0, 1);
  }

  void conversions(Path top, @HasParent Path parent, @HasParent File file) {
    @HasParent File f1 = parent.toFile();
    @HasParent Path p1 = file.toPath();
    // :: error: (assignment)
    @HasName File f2 = top.toFile();
  }

  void fileConstructors(File top, String s) {
    @HasParent File f1 = new File(top, "a/b");
    @HasName File f2 = new File(top, "a");
    // :: error: (assignment)
    @HasParent File f3 = new File(top, "a");
    @HasParent File f4 = new File(s, "a/b");
    @HasName File f5 = new File(s, "a");
    // :: error: (assignment)
    @HasName File f6 = new File(top, s);
    // :: error: (assignment)
    @HasName File f7 = new File(top, "");
    // :: error: (assignment)
    @HasName File f8 = new File(s);
  }

  void regularFile(Path p, File f) {
    if (Files.isRegularFile(p)) {
      @HasName Path p1 = p;
    } else {
      // :: error: (assignment)
      @HasName Path p2 = p;
    }
    // :: error: (assignment)
    @HasName Path p3 = p;
    if (f.isFile()) {
      @HasName File f1 = f;
    }
    // :: error: (assignment)
    @HasName File f2 = f;
    if (!Files.isRegularFile(p)) {
      return;
    }
    @HasName Path p4 = p;
  }

  void tempFiles(Path top, @HasName Path name) throws IOException {
    @HasName Path t1 = Files.createTempFile("p", "s");
    // :: error: (assignment)
    @HasParent Path t2 = Files.createTempFile("p", "s");
    @HasName Path t3 = Files.createTempDirectory("p");
    // :: error: (assignment)
    @HasParent Path t4 = Files.createTempDirectory("p");
    @HasParent Path t5 = Files.createTempFile(name, "p", "s");
    @HasParent Path t6 = Files.createTempDirectory(name, "p");
    @HasName Path t7 = Files.createTempFile(top, "p", "s");
    // :: error: (assignment)
    @HasParent Path t8 = Files.createTempFile(top, "p", "s");
    @HasName Path t9 = Files.createTempDirectory(top, "p");
    // :: error: (assignment)
    @HasParent Path t10 = Files.createTempDirectory(top, "p");
  }
}
