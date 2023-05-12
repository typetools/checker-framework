import org.checkerframework.checker.signedness.qual.UnknownSignedness;

public final class BooleansTest {

  private static int indexOf(boolean[] array, boolean target, int start, int end) {
    for (int i = start; i < end; i++) {
      if (array[i] == target) {
        return i;
      }
    }
    return -1;
  }

  static class BooleanArrayAsList {
    boolean[] array = new boolean[] {};

    int start = 0;
    int end = 0;

    public boolean contains(@UnknownSignedness Object target) {
      return (target instanceof Boolean)
          && BooleansTest.indexOf(array, (Boolean) target, start, end) != -1;
    }
  }
}
