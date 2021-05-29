import java.util.Arrays;
import java.util.Set;

public class SuperRawness {
  // :: warning: [unchecked] Possible heap pollution from parameterized vararg type
  // java.util.Set<X>
  static <X> void test(Set<X>... args) {
    test2(Arrays.asList(args));
  }

  static <X> void test2(Iterable<Set<X>> args) {}
}
