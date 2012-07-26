import java.util.Set;
import java.util.Arrays;

class SuperRawness {
  static <X> void test(Set<X>... args) { test2(Arrays.asList(args)); }
  static <X> void test2(Iterable<Set<X>> args) {}
}
