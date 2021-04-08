import java.util.Arrays;
import org.checkerframework.dataflow.qual.Pure;

public class DaikonCrash {
  void method(Object[] a1) {
    int[] u = union(new int[] {}, new int[] {});
    Arrays.sort(u);
  }

  @Pure
  private int[] union(int[] ints, int[] ints1) {
    throw new RuntimeException();
  }
}
