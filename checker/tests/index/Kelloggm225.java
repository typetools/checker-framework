import org.checkerframework.checker.index.qual.*;
import org.checkerframework.common.value.qual.*;

public class Kelloggm225 {
  void method(int @MinLen(1) [] bar) {
    foo(bar, 0, bar.length);
  }

  void foo(
      int @MinLen(1) [] bar,
      @IndexFor("#1") @LessThan("#3") int start,
      @IndexOrHigh("#1") int end) {}
}
