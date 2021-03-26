// Test case for issue #580: https://github.com/typetools/checker-framework/issues/580

abstract class InitCheckAssertionFailure {
  public static <F extends Enum<F>> void noneOf(F[] array) {
    Enum<?>[] universe = array;
    // Accessing universe on this line causes the error.
    int len = universe.length;
  }
}
