// Tests that -AnoPathShape disables the Nullness Checker's path-based refinement.

import java.io.File;
import java.nio.file.Path;

public class NoPathShape {

  void paths(Path p) {
    // :: error: (dereference.of.nullable)
    Path.of("a/b").getParent().toString();
    // :: error: (dereference.of.nullable)
    p.resolve("a").toAbsolutePath().getParent().toString();
    // :: error: (dereference.of.nullable)
    new File("a").getAbsoluteFile().getParentFile().toString();
  }
}
