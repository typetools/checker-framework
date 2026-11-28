import java.util.ArrayList;
import java.util.List;

// JavaCodeStatistics issues no diagnostics.  This test checks that it does not crash.
public class JavaCodeStatisticsCode {

  @SuppressWarnings({"unchecked", "index:array.access.unsafe.high"})
  List<String> m(Object o, int[] a) {
    a[0] = 1;
    return (List<String>) o;
  }

  List<Integer> makeList() {
    return new ArrayList<>();
  }
}
