// Classes that mention no Path or File type and write no path shape qualifier, but whose member
// uses have unusual receivers. Determining whether to skip such a class must not crash.

import java.util.List;

public class PathShapeReceiverEdgeCases {
  class Base<T> {
    Base(T x) {}
  }
}

// A qualified superclass constructor call: `o` is the enclosing instance, not the receiver.
class PathShapeReceiverEdgeCasesQualifiedSuper extends PathShapeReceiverEdgeCases.Base<String> {
  PathShapeReceiverEdgeCasesQualifiedSuper(PathShapeReceiverEdgeCases o) {
    o.super("");
  }
}

// A member access through a receiver whose type has a wildcard type argument.
class PathShapeReceiverEdgeCasesWildcard {
  Object m(List<?> xs) {
    return xs.get(0);
  }

  Object m2(List<? extends Number> xs) {
    return xs.get(0);
  }

  void m3(List<? super Integer> xs) {
    xs.add(1);
  }
}
