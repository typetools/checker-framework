import org.checkerframework.checker.nullness.qual.*;

import java.util.Arrays;
import java.util.List;

public class ArrayRefs {

    public void test() {

        String[] s = null;

        // :: error: (dereference.of.nullable)
        if (s.length > 0) {
            System.out.println("s.length > 0");
        }
    }

    public static void test2() {
        Object a = new Object();
        takeNNList(Arrays.<Object>asList(new Object[] {a}));
        takeNNList(Arrays.asList(new Object[] {a}));
        takeNNList(Arrays.asList(a, a, a));
        // :: error: (argument.type.incompatible)
        takeNNList(Arrays.asList(a, a, null));
    }

    static void takeNNList(List<Object> p) {}

    <T> void test(T[] a) {
        test(a);
    }

    List<Object> @Nullable [] antecedents_for_suppressors() {
        return null;
    }

    public void find_suppressed_invs() {
        List<Object>[] antecedents = antecedents_for_suppressors();
    }
}
