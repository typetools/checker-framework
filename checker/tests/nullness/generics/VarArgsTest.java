import java.util.Arrays;
import java.util.Set;

public class VarArgsTest {
    // :: warning: [unchecked] Possible heap pollution from parameterized vararg type
    // java.util.Set<? super X>
    <X> void test(Set<? super X>... args) {
        Arrays.asList(args);
    }
    //  static <X> void test(Set<? super X>... args) { test2(Arrays.asList(args)); }
    //  static <X> void test2(Iterable<Set<? super X>> args) {}
}
