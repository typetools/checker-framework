// Test case for issue #783:
// https://github.com/typetools/checker-framework/issues/783

// This file suffers no type-checking warning, but Issue783.java does, and
// it differs only in the import statement.

// You may find it helpful to run this as:
// $ch/bin/javac -cp $ch/dist/checker.jar -processor nullness -AprintVerboseGenerics Issue783b.java

import org.checkerframework.checker.nullness.qual.Nullable;

public class Issue783a<T> {
  @Nullable private T val;

  public void set(@Nullable T val) {
    this.val = val;
  }

  @Nullable
  public T get() {
    return val;
  }
}
