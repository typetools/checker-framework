// Tests the Path Shape Checker's rules for directory listings, and its handling of qualified type
// arguments.

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
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
    @HasParent File @Nullable [] fs7 = name.listFiles();
    if (fs7 != null) {
      for (File f : fs7) {
        f.getParentFile().toString();
      }
    }
    // A local variable does not take the array component type of its initializer.
    File[] fs8 = name.listFiles();
    if (fs8 != null) {
      for (File f : fs8) {
        // :: error: (dereference.of.nullable)
        f.getParentFile().toString();
      }
    }
  }

  // The listing is a fresh array, so storing any File into it is safe.
  void storeIntoListing(File dir) throws IOException {
    File[] files = dir.listFiles();
    if (files != null && files.length > 0) {
      files[0] = files[0].getCanonicalFile();
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
    // :: error: (assignment)
    List<Path> l2 = l;
    var l3 = l;
    // :: error: (argument)
    l3.add(Path.of("/"));
  }

  void takeList(List<Path> l) {}

  // An effectively final local variable takes the covariant type arguments of its initializer.
  void localVariables(Path d) throws IOException {
    try (Stream<Path> s = Files.list(d.resolve("a"))) {
      s.forEach(p -> p.getParent().toString());
    }
    Stream<Path> s2 = Files.list(d);
    @HasName Path p2 = s2.findFirst().get();
    // :: error: (assignment)
    @HasParent Path p3 = Files.list(d).findFirst().get();
    // The type argument of List is invariant, so it is not refined.
    List<Path> l = Files.list(d.resolve("a")).toList();
    // :: error: (dereference.of.nullable)
    l.get(0).getParent().toString();
    takeList(l);
    // A variable declared with `var` has the type of its initializer.
    var l2 = Files.list(d.resolve("a")).toList();
    l2.get(0).getParent().toString();
  }

  // Each local variable is refined from the refined type of the previous one.
  void chain(Path d) throws IOException {
    Stream<Path> a = Files.list(d.resolve("a"));
    Stream<Path> b = a.filter(p -> true);
    Stream<Path> c = b.sorted();
    c.forEach(p -> p.getParent().toString());
  }

  // The initializer is re-examined on each iteration of a loop.
  void loop(boolean b) throws IOException {
    Path d = Path.of("a");
    while (b) {
      Stream<Path> s = Files.list(d);
      // :: error: (dereference.of.nullable)
      s.forEach(p -> p.getParent().toString());
      d = Path.of("");
    }
  }

  // Optional.orElse takes an argument whose type is the type argument.  These are false positives.
  Path orElse(Path dir) throws IOException {
    // :: error: (argument)
    Path p1 = Files.list(dir).findFirst().orElse(dir);
    Optional<Path> first = Files.list(dir).findFirst();
    // :: error: (argument)
    Path p2 = first.orElse(dir);
    return ((Optional<Path>) Files.list(dir).findFirst()).orElse(dir);
  }

  // A qualifier that the programmer wrote is not refined.
  void explicitQualifiers(@HasParent Path[] hasParentArray) {
    @HasName Path[] array = hasParentArray;
    array[0] = Path.of("x");
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

  // Stream.toList() returns an unmodifiable list, so its type argument is treated as covariant.
  List<Path> toList(Path d) throws IOException {
    return Files.list(d).toList();
  }

  List<Path> toListOfLocal(Path dir) throws IOException {
    try (Stream<Path> s = Files.list(dir)) {
      takeList(s.toList());
      List<Path> l = s.toList();
      Collection<Path> c = s.toList();
      List<@HasName Path> named = s.toList();
      // :: error: (assignment)
      List<@HasParent Path> parents = s.toList();
      return s.toList();
    }
  }
}
