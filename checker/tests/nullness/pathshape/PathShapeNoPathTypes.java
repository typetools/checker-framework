// The Path Shape Checker skips a class that does not use paths, but it checks a class that writes
// a path shape qualifier, even if the class mentions no Path or File type.

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import org.checkerframework.checker.nullness.qual.HasName;
import org.checkerframework.checker.nullness.qual.HasParent;

public class PathShapeNoPathTypes {

  void m(Object o) {
    // :: error: (assignment)
    @HasName Object h = o;
  }
}

// This class mentions Path only via the types of other classes' methods, as a wildcard bound and as
// a parameter type.  It is still checked.
class PathShapeWildcardClient {
  void m(PathShapeWildcardApi o) {
    // :: error: (argument)
    o.consume(o.paths().iterator().next());
  }
}

class PathShapeWildcardApi {
  List<? extends Path> paths() {
    return Collections.emptyList();
  }

  void consume(@HasParent Path p) {}
}
