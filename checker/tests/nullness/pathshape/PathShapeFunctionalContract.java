// The Path Shape Checker checks a class that mentions no Path or File type and writes no path shape
// qualifier, if the class contains a lambda or method reference whose functional interface method,
// or whose referenced method, has a path shape qualifier.

import org.checkerframework.checker.nullness.qual.HasName;

public class PathShapeFunctionalContract {
  interface Maker {
    @HasName Object make();
  }

  interface Taker {
    void take(Object o);
  }

  static void takeNamed(@HasName Object o) {}
}

class PathShapeFunctionalContractLambda {
  // :: error: (return)
  PathShapeFunctionalContract.Maker m = () -> new Object();
}

class PathShapeFunctionalContractMethodRef {
  static Object makeObject() {
    return new Object();
  }

  // :: error: (methodref.return)
  PathShapeFunctionalContract.Maker m = PathShapeFunctionalContractMethodRef::makeObject;
}

class PathShapeFunctionalContractReferencedMethod {
  // :: error: (methodref.param)
  PathShapeFunctionalContract.Taker t = PathShapeFunctionalContract::takeNamed;
}
