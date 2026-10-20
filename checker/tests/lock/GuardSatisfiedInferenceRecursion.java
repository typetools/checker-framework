// Test case for a StackOverflowError when a @GuardSatisfied method has a poly-expression argument.

import java.util.ArrayList;
import java.util.List;
import org.checkerframework.checker.lock.qual.GuardSatisfied;

public class GuardSatisfiedInferenceRecursion {
  static <T> @GuardSatisfied(1) List<T> idList(@GuardSatisfied(1) List<T> o) {
    return o;
  }

  static <T> List<T> makeList() {
    return new ArrayList<>();
  }

  void test() {
    List<String> l1 = idList(makeList());
    List<String> l2 = idList(new ArrayList<>());
  }
}
