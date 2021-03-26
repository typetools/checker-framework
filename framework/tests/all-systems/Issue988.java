// Test case for Issue 988:
// https://github.com/typetools/checker-framework/issues/988

abstract class Issue988 {
  abstract Class getRawClass();

  abstract Class<?> getGenericClass();

  Class<?> getWithArg(boolean generic) {
    return generic ? getGenericClass() : getRawClass();
  }
}
