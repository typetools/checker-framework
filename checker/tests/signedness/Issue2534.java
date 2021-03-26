import org.checkerframework.checker.signedness.qual.Unsigned;
import org.checkerframework.common.value.qual.IntRange;

public class Issue2534 {

  @IntRange(from = 0, to = Integer.MAX_VALUE) int field = 3;

  @IntRange(from = 0, to = Integer.MAX_VALUE) int qwe() {
    return 3;
  }

  void m1() {
    @Unsigned int c = qwe();
  }

  void m2() {
    @Unsigned int c = field;
  }

  void m3() {
    @Unsigned int c = this.field;
  }

  void m4(@IntRange(from = 0, to = Integer.MAX_VALUE) int array[]) {
    @Unsigned int c = array[0];
  }
}
