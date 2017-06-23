// Test case for issue 146: https://github.com/kelloggm/checker-framework/issues/146

import java.util.Arrays;
import org.checkerframework.checker.index.qual.*;

class ObjectClone {

    void test(int[] a, int @SameLen("#1") [] b) {
        int @SameLen("a") [] c = b.clone();
        int @SameLen({"a", "d"}) [] d = b.clone();
        int @SameLen({"a", "e"}) [] e = b;
        int @SameLen("f") [] f = b;
    }

    public static void main(String[] args) {
        String @SameLen("args") [] args2 = args;
        String @SameLen({"args", "args_sorted"}) [] args_sorted = args.clone();
        Arrays.sort(args_sorted);
        String @SameLen({"args", "args_sorted"}) [] args_sorted2 = args_sorted.clone();
        if (args_sorted.length == 1) {
            @IndexFor("args_sorted") int i = 0;
            @IndexFor("args") int j = 0;
            String @SameLen({"args", "args_sorted"}) [] k = args;
            System.out.println(args[0]);
        }
    }
}
