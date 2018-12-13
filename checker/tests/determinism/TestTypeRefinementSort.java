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

    void testSort4(
            @OrderNonDet List<@OrderNonDet List<@Det Integer>> ondetList,
            @Det Comparator<@OrderNonDet List<@Det Integer>> cmp) {
        ondetList.sort(cmp);
        // ::error: argument.type.incompatible
        System.out.println(ondetList);
    }

    void testSort5(@PolyDet List<@Det Integer> polyList, @Det Comparator<@Det Integer> cmp) {
        polyList.sort(cmp);
        @PolyDet("down") List<@Det Integer> tmp = polyList;
    }

    void testSort6(
            @PolyDet List<@PolyDet Integer> polyList, @Det Comparator<@PolyDet Integer> cmp) {
        polyList.sort(cmp);
        // ::error: assignment.type.incompatible
        @PolyDet("down") List<@Det Integer> tmp = polyList;
    }

    void testSort7(@Det List<@Det Integer> detList, @Det List<@Det Integer> check) {
        Collections.sort(detList);
        System.out.println(detList.equals(check));
    }

    void testSort8(@NonDet List<@Det Integer> ndetList, @Det List<@Det Integer> check) {
        Collections.sort(ndetList);
        // ::error: argument.type.incompatible
        System.out.println(ndetList.equals(check));
    }

    void testSort9(@OrderNonDet List<@Det Integer> ondetList, @Det List<@Det Integer> check) {
        Collections.sort(ondetList);
        System.out.println(ondetList);
    }

    void testSort10(
            @OrderNonDet List<@OrderNonDet List<@Det Integer>> ondetList,
            @Det Comparator<@OrderNonDet List<@Det Integer>> cmp) {
        Collections.sort(ondetList, cmp);
        // ::error: argument.type.incompatible
        System.out.println(ondetList);
    }

    void testSort11(@PolyDet List<@Det Integer> polyList) {
        Collections.sort(polyList);
        @PolyDet("down") List<@Det Integer> tmp = polyList;
    }

    // TODO: Uncomment this when @ThisDet is implemented and Integer implements Comparable<@ThisDet
    // Integer> rather than Comparable<@Det Integer>.

    // void testSort12(
    //         @PolyDet List<@PolyDet Integer> polyList) {
    //     Collections.sort(polyList);
    //     // ::error: assignment.type.incompatible
    //     @PolyDet("down") List<@Det Integer> tmp = polyList;
    // }

    <T> void sortGeneric1(
            @OrderNonDet List<@Det T> list, @Det Comparator<@Det T> cmp, @Det List<@Det T> check) {
        list.sort(cmp);
        System.out.println(list.equals(check));
    }

    <T> void sortGeneric2(@NonDet List<@Det T> list, @Det Comparator<@Det T> cmp) {
        list.sort(cmp);
        // ::error: argument.type.incompatible
        System.out.println(list);
    }

    <T> void sortGeneric3(@PolyDet List<@Det T> list, @Det Comparator<@Det T> cmp) {
        list.sort(cmp);
        @PolyDet("down") List<@Det T> tmp = list;
    }

    <T extends @Det Object> void sortGeneric4(@OrderNonDet List<T> list, @Det Comparator<T> cmp) {
        list.sort(cmp);
        System.out.println(list);
    }
}
