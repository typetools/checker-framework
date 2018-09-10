import java.util.*;
import org.checkerframework.checker.determinism.qual.*;

public class TestTypeRefinementSort {
    void testSort(
            @Det List<@Det Integer> detList,
            @Det Comparator<@Det Integer> cmp,
            @Det List<@Det Integer> check) {
        detList.sort(cmp);
        System.out.println(detList.equals(check));
    }

    void testSort1(
            @Det List<@Det Integer> detList,
            @NonDet Comparator<@Det Integer> cmp,
            @Det List<@Det Integer> check) {
        // ::error: argument.type.incompatible
        detList.sort(cmp);
        System.out.println(detList.equals(check));
    }

    void testSort2(
            @NonDet List<@Det Integer> ndetList,
            @NonDet Comparator<@Det Integer> cmp,
            @Det List<@Det Integer> check) {
        ndetList.sort(cmp);
        // ::error: argument.type.incompatible
        System.out.println(ndetList.equals(check));
    }

    void testSort3(
            @OrderNonDet List<@Det Integer> ondetList,
            @Det Comparator<@Det Integer> cmp,
            @Det List<@Det Integer> check) {
        ondetList.sort(cmp);
        System.out.println(ondetList);
    }
}
