// Purity annotations are inherited, so an overriding method is checked against the purity
// annotations of the methods it overrides, even if it has no purity annotation of its own.

import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;

public class PurityOverride {

  static class Super {
    @Pure
    int pure() {
      return 1;
    }

    @SideEffectFree
    int sideEffectFree() {
      return 1;
    }
  }

  static class ImpureSub extends Super {
    int field = 0;

    @Override
    int pure() {
      // :: error: [purity.assign.field]
      field++;
      return field;
    }

    @Override
    int sideEffectFree() {
      // :: error: [purity.assign.field]
      field++;
      return field;
    }
  }

  static class PureSub extends Super {
    @Override
    int pure() {
      return 2;
    }

    @Override
    int sideEffectFree() {
      return 2;
    }
  }

  // `Object.hashCode()` is `@Pure`.
  static class ImpureHashCode {
    int field = 0;

    @Override
    public int hashCode() {
      // :: error: [purity.assign.field]
      field++;
      return field;
    }
  }
}
