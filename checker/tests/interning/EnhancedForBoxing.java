import java.util.List;
import org.checkerframework.checker.interning.qual.Interned;

// An enhanced for loop that boxes or unboxes its elements.  A boxed value is not interned, except
// for a boxed boolean or byte.
public class EnhancedForBoxing {
  void boxArray(int[] ints, boolean[] bools, byte[] bytes) {
    for (Integer x : ints) {
      // :: error: [assignment]
      @Interned Integer y = x;
    }
    for (Object x : ints) {
      // :: error: [assignment]
      @Interned Object y = x;
    }
    for (Boolean x : bools) {
      @Interned Boolean y = x;
    }
    for (Byte x : bytes) {
      @Interned Byte y = x;
    }
  }

  void boxArrayLiteral() {
    for (Integer x : new int[] {1000}) {
      // :: error: [assignment]
      @Interned Integer y = x;
    }
  }

  void unbox(Integer[] ints, List<Integer> list) {
    for (int x : ints) {
      @Interned int y = x;
    }
    for (int x : list) {
      @Interned int y = x;
    }
  }
}
