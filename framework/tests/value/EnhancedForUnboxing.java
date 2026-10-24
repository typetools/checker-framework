import java.util.List;
import org.checkerframework.common.value.qual.IntVal;

// The loop variable gets the element type of the iterable expression, even when assignment
// conversion unboxes or boxes the element.
public class EnhancedForUnboxing {
  void unboxList(List<@IntVal(1) Integer> list) {
    for (int x : list) {
      @IntVal(1) int y = x;
      // :: error: [assignment]
      @IntVal(2) int z = x;
    }
    for (long x : list) {
      @IntVal(1) long y = x;
      // :: error: [assignment]
      @IntVal(2) long z = x;
    }
  }

  void unboxArray(@IntVal(1) Integer[] array) {
    for (int x : array) {
      @IntVal(1) int y = x;
      // :: error: [assignment]
      @IntVal(2) int z = x;
    }
    for (long x : array) {
      @IntVal(1) long y = x;
      // :: error: [assignment]
      @IntVal(2) long z = x;
    }
  }

  void boxArray(@IntVal(1) int[] array) {
    for (Integer x : array) {
      @IntVal(1) Integer y = x;
      // :: error: [assignment]
      @IntVal(2) Integer z = x;
    }
    for (Number x : array) {
      @IntVal(1) Number y = x;
      // :: error: [assignment]
      @IntVal(2) Number z = x;
    }
  }
}
