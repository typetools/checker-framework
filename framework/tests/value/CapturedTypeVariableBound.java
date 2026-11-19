import java.util.List;
import org.checkerframework.common.value.qual.IntVal;

// A value whose type is a captured type variable, such as the type of `list.get(0)` for a
// `List<? extends U>`, has the qualifiers of the type variable that bounds the captured wildcard.
public class CapturedTypeVariableBound {
  <U extends @IntVal(1) Integer> void boxed(List<? extends U> list) {
    Integer z = list.get(0);
    @IntVal(1) Integer y = z;
    // :: error: [assignment]
    @IntVal(2) Integer w = z;
  }

  <U extends @IntVal(1) Integer> void unboxed(List<? extends U> list) {
    int z = list.get(0);
    @IntVal(1) int y = z;
    // :: error: [assignment]
    @IntVal(2) int w = z;
  }

  <T extends @IntVal(1) Integer, U extends T> void chained(List<? extends U> list) {
    int z = list.get(0);
    @IntVal(1) int y = z;
  }
}
