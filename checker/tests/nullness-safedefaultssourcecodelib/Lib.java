// A library to be compiled with
//   -AuseConservativeDefaultsForUncheckedCode=source,bytecode
// and then to be read from bytecode.

import org.checkerframework.checker.nullness.qual.*;
import org.checkerframework.framework.qual.AnnotatedFor;

@AnnotatedFor("nullness")
class SdfuscLib {
  static Object unannotated() {
    return new Object();
  }

  static @Nullable Object returnsNullable() {
    return new Object();
  }

  static @NonNull Object returnsNonNull() {
    return new Object();
  }
}

class SdfuscLibNotAnnotatedFor {
  static Object unannotated() {
    return new Object();
  }

  static @Nullable Object returnsNullable() {
    return new Object();
  }

  static @NonNull Object returnsNonNull() {
    return new Object();
  }
}
