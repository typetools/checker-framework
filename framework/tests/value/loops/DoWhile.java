import org.checkerframework.common.value.qual.IntRange;
import org.checkerframework.common.value.qual.IntVal;

// Because the analysis of loops isn't precise enough, the Value Checker issues
// warnings on this test case. So, suppress those warnings, but run the tests
// to make sure that dataflow reaches a fixed point.
@SuppressWarnings("value")
public class DoWhile {

  void doWhile() {
    int d = 0;
    do {
      d++;
    } while (d < 399);
    @IntRange(from = 399) int after = d;
  }

  void another() {
    int d = 0;
    do {
      d++;
      if (d > 444) {
        break;
      }
      @IntRange(from = 1, to = 444) int z = d;
    } while (true);
  }

  void fromAnno(@IntVal(2222) int param) {
    int d = 0;
    do {
      d++;
      if (d > param) {
        break;
      }
      @IntRange(from = 1, to = 2222) int z = d;
    } while (true);
  }
}
