// @below-java22-jdk-skip-test

// Tests Path.resolve(String, String...) and Path.resolve(Path, Path...), added in Java 22.

import java.nio.file.Path;
import org.checkerframework.checker.nullness.qual.HasName;
import org.checkerframework.checker.nullness.qual.HasParent;

public class PathShapeResolveVarargs {

  void resolveVarargs(Path top, @HasName Path name, String s) {
    @HasParent Path p1 = top.resolve("a", "b");
    @HasParent Path p2 = top.resolve(name, name);
    @HasName Path p3 = top.resolve("", "b");
    // :: error: (assignment)
    @HasName Path p4 = name.resolve("a", s);
  }
}
