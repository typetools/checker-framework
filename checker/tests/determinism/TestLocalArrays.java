import java.util.ArrayList;
import org.checkerframework.checker.determinism.qual.*;

public class TestLocalArrays<T> {
    void testarr(@NonDet int @NonDet [] a) {
        // :: error: (assignment.type.incompatible)
        @Det int @Det [] b = a;
    }

    void testelm(@NonDet int @NonDet [] a) {
        // :: error: (assignment.type.incompatible)
        @Det int x = a[0];
    }

    void testTypeParameters(@NonDet ArrayList<@NonDet Integer> a) {
        // :: error: (assignment.type.incompatible)
        @Det ArrayList<@Det Integer> b = a;
    }

    <@Det T> void issue392(@Det T t) {
        Object o = new @Det Object @Det [] {t, t};
    }

    void createOndArray() {
        @Det int @OrderNonDet [] ar = new int[20];
        // :: error: (ordernondet.on.noncollection)
        @OrderNonDet int @OrderNonDet [] invalidAr;

        @NonDet int @NonDet [] nar = new @NonDet int @NonDet [20];

        @Det int @OrderNonDet [] @OrderNonDet [] oar = new @Det int @OrderNonDet [10] @OrderNonDet [10];
    }

    void assignElement1(@Det int @OrderNonDet [] a, @Det int x, @Det int index) {
        a[index] = x;
    }

    void assignElement1_2D(@Det int @OrderNonDet [] @OrderNonDet [] a, @Det int x, @Det int index) {
        a[index][index] = x;
        a[index] = new @Det int @OrderNonDet [10];
    }

    void assignElement2(@Det int @OrderNonDet [] a, @Det int x, @NonDet int index) {
        // :: error: (invalid.array.assignment)
        a[index] = x;
        // :: error: (assignment.type.incompatible)
        a[x] = index;
    }

    void assignElement2_2D(
            @Det int @OrderNonDet [] @OrderNonDet [] a, @Det int x, @NonDet int index) {
        // :: error: (invalid.array.assignment)
        a[index][index] = x;
        // :: error: (assignment.type.incompatible)
        a[x][x] = index;
        // :: error: (assignment.type.incompatible)
        a[x] = new @NonDet int @NonDet [5];
    }

    void assignRhs1(@NonDet int @NonDet [] a) {
        // :: error: (assignment.type.incompatible)
        @Det int x = a[0];
    }

    void assignRhs1_2D(@NonDet int @NonDet [] @NonDet [] a) {
        // :: error: (assignment.type.incompatible)
        @Det int x = a[0][4];
    }

    void assignRhs2(@Det int @NonDet [] a) {
        // :: error: (assignment.type.incompatible)
        @Det int x = a[0];
    }

    void assignRhs2_2D(@Det int @NonDet [] @NonDet [] a) {
        // :: error: (assignment.type.incompatible)
        @Det int x = a[0][0];
    }

    void accessLength(@NonDet int @NonDet [] a) {
        // :: error: (assignment.type.incompatible)
        @Det int l = a.length;
    }

    void methInv(@Det int @NonDet [] a, @Det int index) {
        // :: error: (argument.type.incompatible)
        arrMethod(a[index]);
    }

    void arrMethod(@Det int x) {}
}
