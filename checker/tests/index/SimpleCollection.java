import org.checkerframework.checker.index.qual.*;

public class SimpleCollection {
  private int[] values;

  @IndexOrHigh("values") int size() {
    return values.length;
  }

  void interact_with_other(SimpleCollection other) {
    int[] othervalues = other.values;
    int @SameLen("other.values") [] x = othervalues;
    for (int i = 0; i < other.size(); i++) {
      int k = othervalues[i];
    }
    for (int j = 0; j < other.size(); j++) {
      int k = other.values[j];
    }
  }
}
