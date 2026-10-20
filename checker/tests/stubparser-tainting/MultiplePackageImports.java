// Tests that an import that appears after a package declaration in a stub file applies to the
// whole stub file.  In MultiplePackageImports.astub, @Untainted is imported after the first
// package declaration, and the annotation is used in the second package declaration.

import java.util.UUID;
import org.checkerframework.checker.tainting.qual.Untainted;

public class MultiplePackageImports {
  void m(UUID uuid, Object o) {
    @Untainted String s = uuid.toString();
    // :: error: (assignment)
    @Untainted String t = o.toString();
  }
}
