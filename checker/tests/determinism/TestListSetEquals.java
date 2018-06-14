import java.util.*;
import org.checkerframework.checker.determinism.qual.*;

public class TestListSetEquals {
    void testListEquals1(@Det List<@Det Integer> aList, @Det List<@Det Integer> bList) {
        @Det boolean ret = aList.equals(bList);
    }

    void testListEquals2(@Det List<@Det Integer> aList, @OrderNonDet List<@Det Integer> bList) {
        // :: error: (assignment.type.incompatible)
        @Det boolean ret = aList.equals(bList);
    }

    void testListEquals3(@Det List<@Det Integer> aList, @NonDet List<@Det Integer> bList) {
        // :: error: (assignment.type.incompatible)
        @Det boolean ret = aList.equals(bList);
    }

    void testListEquals4(@OrderNonDet List<@Det Integer> aList, @Det List<@Det Integer> bList) {
        // :: error: (assignment.type.incompatible)
        @Det boolean ret = aList.equals(bList);
    }

    void testListEquals5(
            @OrderNonDet List<@Det Integer> aList, @OrderNonDet List<@Det Integer> bList) {
        // :: error: (assignment.type.incompatible)
        @Det boolean ret = aList.equals(bList);
    }

    void testListEquals6(@OrderNonDet List<@Det Integer> aList, @NonDet List<@Det Integer> bList) {
        // :: error: (assignment.type.incompatible)
        @Det boolean ret = aList.equals(bList);
    }

    void testListEquals7(@NonDet List<@NonDet Integer> aList, @Det List<@Det Integer> bList) {
        // :: error: (assignment.type.incompatible)
        @Det boolean ret = aList.equals(bList);
    }

    void testListEquals8(
            @NonDet List<@NonDet Integer> aList, @OrderNonDet List<@Det Integer> bList) {
        // :: error: (assignment.type.incompatible)
        @Det boolean ret = aList.equals(bList);
    }

    void testListEquals9(@NonDet List<@NonDet Integer> aList, @NonDet List<@NonDet Integer> bList) {
        // :: error: (assignment.type.incompatible)
        @Det boolean ret = aList.equals(bList);
    }

    //List equals Set
    void testListSetEquals1(@Det List<@Det Integer> aList, @Det Set<@Det Integer> bList) {
        @Det boolean ret = aList.equals(bList);
    }

    void testListSetEquals2(@Det List<@Det Integer> aList, @OrderNonDet Set<@Det Integer> bList) {
        // :: error: (assignment.type.incompatible)
        @Det boolean ret = aList.equals(bList);
    }

    void testListSetEquals3(@Det List<@Det Integer> aList, @NonDet Set<@Det Integer> bList) {
        // :: error: (assignment.type.incompatible)
        @Det boolean ret = aList.equals(bList);
    }

    void testListSetEquals4(@OrderNonDet List<@Det Integer> aList, @Det Set<@Det Integer> bList) {
        // :: error: (assignment.type.incompatible)
        @Det boolean ret = aList.equals(bList);
    }

    void testListSetEquals5(
            @OrderNonDet List<@Det Integer> aList, @OrderNonDet Set<@Det Integer> bList) {
        // :: error: (assignment.type.incompatible)
        @Det boolean ret = aList.equals(bList);
    }

    void testListSetEquals6(
            @OrderNonDet List<@Det Integer> aList, @NonDet Set<@Det Integer> bList) {
        // :: error: (assignment.type.incompatible)
        @Det boolean ret = aList.equals(bList);
    }

    void testListSetEquals7(@NonDet List<@NonDet Integer> aList, @Det Set<@Det Integer> bList) {
        // :: error: (assignment.type.incompatible)
        @Det boolean ret = aList.equals(bList);
    }

    void testListSetEquals8(
            @NonDet List<@NonDet Integer> aList, @OrderNonDet Set<@Det Integer> bList) {
        // :: error: (assignment.type.incompatible)
        @Det boolean ret = aList.equals(bList);
    }

    void testListSetEquals9(
            @NonDet List<@NonDet Integer> aList, @NonDet Set<@NonDet Integer> bList) {
        // :: error: (assignment.type.incompatible)
        @Det boolean ret = aList.equals(bList);
    }
}
