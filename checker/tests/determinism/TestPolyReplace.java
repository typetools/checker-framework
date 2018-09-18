import java.util.*;
import org.checkerframework.checker.determinism.qual.*;

public class TestPolyReplace {
    <T> void testPolyDown(@OrderNonDet Set<T> set, @Det T elem) {
        @Det boolean out = set.contains(elem);
    }

    @Det int @PolyDet("up") [][] testArrayPolyUp(@Det int @PolyDet [][] arr) {
        return arr;
    }

    @PolyDet("up") int @PolyDet("up") [] @PolyDet("up") [] testArrayPolyUp1(int[][] arr) {
        return arr;
    }

    @Det int @NonDet [][] callArrayPolyUp(@Det int @OrderNonDet [][] arr, int[][] arr1) {
        // :: error: (assignment.type.incompatible)
        @Det int @Det [][] local = testArrayPolyUp(arr);

        // :: error: (assignment.type.incompatible)
        @Det int @Det [][] local1 = testArrayPolyUp1(arr1);
        System.out.println(local1[0][0]);
        return local;
    }

    void checkArrAccess(@Det int @NonDet [][] testArr, @Det int @NonDet [] again) {
        @Det int @NonDet [] temp = testArr[0];
        @Det int check = temp[0];
        // :: error: (assignment.type.incompatible)
        @Det int check1 = again[0];
    }

    @PolyDet("up") List<List<Integer>> polyList(@PolyDet List<List<Integer>> list) {
        return list;
    }

    void callPolyList(@OrderNonDet List<List<Integer>> list) {
        // :: error: (assignment.type.incompatible)
        @Det List<@Det List<@Det Integer>> local = polyList(list);
    }
}
