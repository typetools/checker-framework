import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;

public class SideEffectFreeVoidMethod {

  int size;

  @Pure
  private int getOrNull(int index) {
    assertIndexInBounds(index, "getOrNull");
    return 22;
  }

  @SideEffectFree
  private void assertIndexInBounds(int index, String method) {
    if (index < 0 || index >= size) {
      throw new IndexOutOfBoundsException(
          method + "(" + index + ",...) called on ArrayMap of size " + size);
    }
  }
}
