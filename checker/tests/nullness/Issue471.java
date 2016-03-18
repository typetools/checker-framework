// Test case for Issue 471
// https://github.com/typetools/checker-framework/issues/471
// javacheck -processor nullness Issue471.java -AprintVerboseGenerics -AprintAllQualifiers 
//@skip-test
import javax.annotation.Nullable;

class Issue471<T> {
  @Nullable T t;
  Issue471(@Nullable T t) {
    this.t = t;
  }
}
