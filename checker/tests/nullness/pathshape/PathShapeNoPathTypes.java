// The Path Shape Checker skips a class that does not use paths, but it checks a class that writes
// a path shape qualifier, even if the class mentions no Path or File type.

import org.checkerframework.checker.nullness.qual.HasName;

public class PathShapeNoPathTypes {

  void m(Object o) {
    // :: error: (assignment)
    @HasName Object h = o;
  }
}
