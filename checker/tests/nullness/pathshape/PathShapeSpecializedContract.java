// The Path Shape Checker checks a class that mentions no Path or File type and writes no path shape
// qualifier, if the class uses a member whose type has a path shape qualifier only when viewed as a
// member of the receiver's type, or uses a generic class whose type parameter has a path shape
// bound.

import org.checkerframework.checker.nullness.qual.HasName;
import org.checkerframework.checker.nullness.qual.Nullable;

public class PathShapeSpecializedContract {
  static class Box<T> {
    @Nullable T value;

    void put(T t) {}
  }

  static class NamedBox extends Box<@HasName Object> {}

  static class RequiresNamed<T extends @HasName Object> {}

  interface Maker<T> {
    T make();
  }

  interface NamedMaker extends Maker<@HasName Object> {}
}

class PathShapeSpecializedContractInvocation {
  void m(PathShapeSpecializedContract.NamedBox box) {
    // :: error: (argument)
    box.put(new Object());
  }
}

class PathShapeSpecializedContractField {
  void m(PathShapeSpecializedContract.NamedBox box) {
    // :: error: (assignment)
    box.value = new Object();
  }
}

class PathShapeSpecializedContractImplicitReceiver extends PathShapeSpecializedContract.NamedBox {
  void m() {
    // :: error: (argument)
    put(new Object());
  }
}

class PathShapeSpecializedContractLambda {
  // :: error: (return)
  PathShapeSpecializedContract.NamedMaker m = () -> new Object();
}

class PathShapeSpecializedContractBound {
  // :: error: (type.argument)
  PathShapeSpecializedContract.@Nullable RequiresNamed<Object> field;
}
