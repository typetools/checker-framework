import org.checkerframework.checker.index.qual.LessThan;
import org.checkerframework.common.value.qual.IntRange;
import org.checkerframework.common.value.qual.IntVal;

public class LessThanBug {

  void call() {
    bug(30, 1);
  }

  void bug(@IntRange(to = 42) int a, @IntVal(1) int c) {
    // :: error: (assignment)
    @LessThan("c") int x = a;
  }
}
