// Test case for issue #783:
// https://github.com/typetools/checker-framework/issues/783

// Note that Issue783a.java, which differs only in the import statement,
// suffers no type-checking warning.

// You may find it helpful to run this as:
// $ch/bin/javac -cp $ch/dist/checker.jar -processor nullness -AprintVerboseGenerics Issue783b.java

import javax.annotation.Nullable;

public class Issue783b<T> {
  private @Nullable T val;

  public void set(@Nullable T val) {
    this.val = val;
  }

  @Nullable public T get() {
    return val;
  }
}
