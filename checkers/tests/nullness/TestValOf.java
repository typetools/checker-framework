// Test case for issue #243: https://code.google.com/p/checker-framework/issues/detail?id=243

class TestValOf<T extends Enum<T>> {

  private final Class<T> enumClass;

    private TestValOf(Class<T> enumClass) {
      this.enumClass = enumClass;
    }

  T foo(String value) {
    return Enum.valueOf(enumClass, value);
  }
}
