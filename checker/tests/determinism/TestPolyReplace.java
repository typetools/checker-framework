import java.util.*;
import org.checkerframework.checker.determinism.qual.*;

// @skip-test
public class TestPolyReplace {
    //    <T> void testPolyDown(@OrderNonDet Set<T> set, @Det T elem) {
    //        @Det boolean out = set.contains(elem);
    //    }
    //
    //    @Det int @PolyDet("up") [][] testArrayPolyUp(@Det int @PolyDet [][] arr) {
    //        return arr;
    //    }
    //
    //    @Det int @NonDet [][] callArrayPolyUp(@Det int @OrderNonDet [][] arr) {
    //        @Det int @NonDet [][] local = testArrayPolyUp(arr);
    //        System.out.println(local[0][0]);
    //        return local;
    //    }
    //
    //    void checkArrAccess(@Det int @NonDet [][] testArr, @Det int @NonDet [] again) {
    //        @Det int @NonDet [] temp = testArr[0];
    //        @Det int check = temp[0];
    //        @Det int check1 = again[0];
    //    }
    //
    @PolyDet("up") List<List<Integer>> polyList(@PolyDet List<List<Integer>> list) {
        return list;
    }

    void callPolyList(@OrderNonDet List<List<Integer>> list) {
        @Det List<@Det List<@Det Integer>> local = polyList(list);
    }
}
