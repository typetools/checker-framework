import java.util.List;
import checkers.nullness.quals.*;

class AaTest<A extends @Nullable Object> {
  void test(List<? extends A> list) { test2(list.get(0)); }
  void test2(A x) {};
}
