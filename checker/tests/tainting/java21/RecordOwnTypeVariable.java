// @below-java21-jdk-skip-test

// @infer-jaifs-skip-test
// @infer-ajava-skip-test
// @infer-stubs-skip-test

// Within a generic record, a matched type can mention the record's own type parameter.  Inference
// replaces only captured type variables in the matched type, so the record's type parameter is the
// type argument of the record pattern, not the bound of that type parameter.

import org.checkerframework.checker.tainting.qual.Untainted;

public class RecordOwnTypeVariable {

  record Box<T>(T value) {
    T unbox(Box<T> b) {
      if (b instanceof Box(var x)) {
        T t = x;
        return x;
      }
      throw new Error();
    }

    T unboxSwitch(Box<T> b) {
      return switch (b) {
        case Box(var x) -> x;
      };
    }

    T unboxThis() {
      if (this instanceof Box(var x)) {
        return x;
      }
      throw new Error();
    }
  }

  record UntaintedBox<T extends @Untainted Object>(T value) {
    T unbox(UntaintedBox<T> b) {
      if (b instanceof UntaintedBox(var x)) {
        @Untainted Object u = x;
        return x;
      }
      throw new Error();
    }
  }
}
