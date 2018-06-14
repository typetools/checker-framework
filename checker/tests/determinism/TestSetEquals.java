import java.util.*;
import org.checkerframework.checker.determinism.qual.*;

public class TestSetEquals {
    void test(@OrderNonDet Set<@Det Integer> set, @OrderNonDet List<@Det Integer> lst) {
        @NonDet Object obj = (@NonDet Object) lst;
        set.equals(obj);
    }

    void test1(@Det Set<@Det Integer> set, @Det Set<@Det Integer> st) {
        System.out.println(set.equals(st));
    }

    void test2(
            @OrderNonDet ArrayList<@Det Integer> aList,
            @OrderNonDet ArrayList<@Det Integer> bList) {
        // :: error: (assignment.type.incompatible)
        @Det boolean result = aList.equals(bList);
    }

    void test2(@OrderNonDet HashSet<@Det Integer> aSet, @OrderNonDet HashSet<@Det Integer> bSet) {
        // :: error: (assignment.type.incompatible)
        @Det boolean result = aSet.equals(bSet);
    }
}
