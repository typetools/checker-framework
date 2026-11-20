// The Path Shape Checker checks a class that mentions no Path or File type and writes no path shape
// qualifier, if the class overrides or uses a declaration whose type has a path shape qualifier.

import org.checkerframework.checker.nullness.qual.HasName;

public abstract class PathShapeInheritedContract {
  abstract @HasName Object get();

  void put(@HasName Object o) {}
}

class PathShapeInheritedContractSub extends PathShapeInheritedContract {
  @Override
  // :: error: (override.return)
  Object get() {
    return new Object();
  }
}

class PathShapeInheritedContractClient {
  void m(PathShapeInheritedContract c, Object o) {
    // :: error: (argument)
    c.put(o);
  }
}
