// Tests the Path Shape Checker's rules for directory listings, and its handling of qualified type
// arguments.

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.checkerframework.checker.nullness.qual.HasName;
import org.checkerframework.checker.nullness.qual.HasParent;
import org.checkerframework.checker.nullness.qual.Nullable;

public class PathShapeListings {

  void list(Path top, @HasName Path name) throws IOException {
    Stream<@HasName Path> s1 = Files.list(top);
    // :: error: (assignment)
    Stream<@HasParent Path> s2 = Files.list(top);
    Stream<@HasParent Path> s3 = Files.list(name);
    Files.list(name).forEach(p -> p.getParent().toString());
    // :: error: (dereference.of.nullable)
    Files.list(top).forEach(p -> p.getParent().toString());
    Files.list(top).forEach(p -> p.getFileName().toString());
  }

  void newDirectoryStream(Path top, @HasName Path name) throws IOException {
    DirectoryStream<@HasName Path> d1 = Files.newDirectoryStream(top);
    DirectoryStream<@HasParent Path> d2 = Files.newDirectoryStream(name, "*.java");
    DirectoryStream<@HasParent Path> d3 = Files.newDirectoryStream(name, p -> true);
    for (Path p : Files.newDirectoryStream(name)) {
      p.getParent().toString();
    }
    for (Path p : Files.newDirectoryStream(top)) {
      // :: error: (dereference.of.nullable)
      p.getParent().toString();
    }
  }

  void listFiles(File top, @HasName File name) {
    File @Nullable [] fs1 = top.listFiles();
    @HasName File @Nullable [] fs2 = top.listFiles();
    // :: error: (assignment)
    @HasParent File @Nullable [] fs3 = top.listFiles();
    @HasParent File @Nullable [] fs4 = name.listFiles();
    @HasParent File @Nullable [] fs5 = name.listFiles((dir, n) -> true);
    @HasParent File @Nullable [] fs6 = name.listFiles(f -> true);
    File[] fs7 = name.listFiles();
    if (fs7 != null) {
      for (File f : fs7) {
        f.getParentFile().toString();
      }
    }
  }

  void walk(Path top, @HasName Path name, @HasParent Path parent) throws IOException {
    // The first element is the start path itself.
    // :: error: (assignment)
    Stream<@HasName Path> s1 = Files.walk(top);
    Stream<@HasName Path> s2 = Files.walk(name);
    // :: error: (assignment)
    Stream<@HasParent Path> s3 = Files.walk(name);
    Stream<@HasParent Path> s4 = Files.walk(parent, 2);
    Stream<@HasParent Path> s5 = Files.find(parent, 2, (p, attrs) -> true);
  }

  // Type arguments of read-only types are covariant, so code that ignores path shapes is accepted.
  Stream<Path> covariance(Path d) throws IOException {
    Stream<Path> s;
    s = Files.list(d);
    takeStream(Files.list(d));
    Iterable<Path> i = Files.newDirectoryStream(d);
    Iterator<Path> it = Files.newDirectoryStream(d).iterator();
    Optional<Path> o = Files.list(d).findFirst();
    return Files.list(d);
  }

  void takeStream(Stream<Path> s) {}

  // List is not read-only, so its type argument is invariant.
  void invariance(List<@HasName Path> l) {
    // :: error: (argument)
    takeList(l);
    // l2 takes the type arguments of its initializer, as if it were declared with `var`.
    List<Path> l2 = l;
    // :: error: (argument)
    l2.add(Path.of("/"));
  }

  void takeList(List<Path> l) {}

  // An effectively final local variable takes the type arguments of its initializer.
  void localVariables(Path d) throws IOException {
    try (Stream<Path> s = Files.list(d.resolve("a"))) {
      s.forEach(p -> p.getParent().toString());
    }
    List<Path> l = Files.list(d.resolve("a")).toList();
    l.get(0).getParent().toString();
    Stream<Path> s2 = Files.list(d);
    @HasName Path p2 = s2.findFirst().get();
    // :: error: (assignment)
    @HasParent Path p3 = Files.list(d).findFirst().get();
  }

  void localVariableReassigned(Path d) throws IOException {
    Stream<Path> s = Files.list(d.resolve("a"));
    // :: error: (dereference.of.nullable)
    s.forEach(p -> p.getParent().toString());
    s = Stream.of(Path.of("/"));
  }

  void collect(Path d) throws IOException {
    List<Path> l = Files.list(d).collect(Collectors.toList());
    l.add(Path.of("/"));
  }

  // A List is not read-only, so its type argument is invariant.  This is a false positive.
  List<Path> toList(Path d) throws IOException {
    // :: error: (return)
    return Files.list(d).toList();
  }
}
