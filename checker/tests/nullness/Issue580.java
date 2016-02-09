// @skip-test

// Test case for issue #580: https://github.com/typetools/checker-framework/issues/580

import java.util.Set;

abstract class InitCheckAssertionFailure<E> {
  public static <E extends Enum<E>> Set<E> noneOf(Class<E> elementType) {
    Enum<?>[] universe = getEnumConstants(elementType);
    if (universe.length <= 0) {
      throw new RuntimeException();
    }
    return null;
  }

  private static <E> E[] getEnumConstants(Class<E> elementType) {
    return null;
  }
}
