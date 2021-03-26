// Testcase for Issue759
// https://github.com/typetools/checker-framework/issues/759
@SuppressWarnings({"nullness", "unchecked"}) // See checker/test/nullness/Issue759.java
public class Issue759 {
  void possibleValues(final Class<? extends Enum> enumType) {
    lowercase(enumType.getEnumConstants());
    lowercase2(enumType.getEnumConstants());
    lowercase3(enumType.getEnumConstants());
  }

  <T extends Enum<T>> void lowercase(final T... items) {}

  <T extends Enum<T>> void lowercase2(final T[] items) {}

  <T> void lowercase3(final T items) {}
}

@SuppressWarnings("nullness")
class Gen<T extends Gen<T>> {
  T[] getConstants() {
    return null;
  }
}

@SuppressWarnings("nullness")
class IncompatibleTypes {
  void possibleValues(final Gen<?> genType) {
    lowercase(genType.getConstants());
  }

  <S> void lowercase(final S items) {}
}
