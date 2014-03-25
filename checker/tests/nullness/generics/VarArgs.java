import java.util.Set;
import java.util.Arrays;

class AaTest {
  //:: warning: [unchecked] Possible heap pollution from parameterized vararg type java.util.Set<? super X>
  <X> void test(Set<? super X>... args) {
      Arrays.asList(args);
  }
//  static <X> void test(Set<? super X>... args) { test2(Arrays.asList(args)); }
//  static <X> void test2(Iterable<Set<? super X>> args) {}
}
