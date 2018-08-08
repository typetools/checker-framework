import java.util.*;
import org.checkerframework.checker.determinism.qual.*;

public class TestArraysSort {
    void testSort(@Det int @Det [] a) {
        Arrays.sort(a);
    }

    void testSort1(@Det int @OrderNonDet [] a) {
        // :: error: (argument.type.incompatible)
        System.out.println(a[0]);
        Arrays.sort(a);
        System.out.println(a[0]);
    }

    void testSort2(@Det Integer @OrderNonDet [] a) {
        // :: error: (argument.type.incompatible)
        System.out.println(a[0]);
        @Det IntComparator c = new @Det IntComparator();
        Arrays.sort(a);
        Arrays.sort(a, c);
        System.out.println(a[0]);
    }

    void testSort3(@Det Integer @OrderNonDet [] a, @Det Comparator<Integer> c) {
        Arrays.sort(a, c);
        System.out.println(a[0]);
    }

    void testSort4(@Det Integer @OrderNonDet [] a, @NonDet Comparator<Integer> c) {
        // :: error: (argument.type.incompatible)
        Arrays.sort(a, c);
        // :: error: (argument.type.incompatible)
        System.out.println(a[0]);
    }

    void testSort5(@Det Integer @NonDet [] a) {
        @NonDet IntComparator c = new @NonDet IntComparator();
        Arrays.sort(a, c);
    }
}

class IntComparator implements Comparator<@NonDet Integer> {
    public @NonDet int compare(@NonDet Integer i1, @NonDet Integer i2) {
        return 0;
    }
}
