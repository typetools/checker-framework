// The Path Shape Checker checks a class that mentions no Path or File type and writes no path shape
// qualifier, if the class uses a declaration whose path shape qualifier comes from a default.

import org.checkerframework.checker.nullness.qual.HasName;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.checkerframework.framework.qual.TypeUseLocation;

public class PathShapeDefaultedContract {
  @DefaultQualifier(value = HasName.class, locations = TypeUseLocation.PARAMETER)
  static void accept(Object o) {}

  @DefaultQualifier(value = HasName.class, locations = TypeUseLocation.UPPER_BOUND)
  static class RequiresNamed<T> {}
}

class PathShapeDefaultedContractStaticInvocation {
  void m() {
    // :: error: (argument)
    PathShapeDefaultedContract.accept(new Object());
  }
}

class PathShapeDefaultedContractBound {
  // :: error: (type.argument)
  PathShapeDefaultedContract.@Nullable RequiresNamed<Object> field;
}
