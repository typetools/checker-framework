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

    void testListEquals31(@Det List<@Det Integer> aList, @NonDet List<@NonDet Integer> bList) {
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

    //Set equals set
    void testSetEquals1(@Det Set<@Det Integer> aList, @Det Set<@Det Integer> bList) {
        @Det boolean ret = aList.equals(bList);
    }

    void testSetEquals2(@Det Set<@Det Integer> aList, @OrderNonDet Set<@Det Integer> bList) {
        // :: error: (assignment.type.incompatible)
        @Det boolean ret = aList.equals(bList);
    }

    void testSetEquals3(@Det Set<@Det Integer> aList, @NonDet Set<@Det Integer> bList) {
        // :: error: (assignment.type.incompatible)
        @Det boolean ret = aList.equals(bList);
    }

    void testSetEquals4(@OrderNonDet Set<@Det Integer> aList, @Det Set<@Det Integer> bList) {
        // :: error: (assignment.type.incompatible)
        @Det boolean ret = aList.equals(bList);
    }

    void testSetEquals5(
            @OrderNonDet Set<@Det Integer> aList, @OrderNonDet Set<@Det Integer> bList) {
        @Det boolean ret = aList.equals(bList);
    }

    void testSetEquals6(@OrderNonDet Set<@Det Integer> aList, @NonDet Set<@Det Integer> bList) {
        // :: error: (assignment.type.incompatible)
        @Det boolean ret = aList.equals(bList);
    }

    void testSetEquals7(@NonDet Set<@NonDet Integer> aList, @Det Set<@Det Integer> bList) {
        // :: error: (assignment.type.incompatible)
        @Det boolean ret = aList.equals(bList);
    }

    void testSetEquals8(@NonDet Set<@NonDet Integer> aList, @OrderNonDet Set<@Det Integer> bList) {
        // :: error: (assignment.type.incompatible)
        @Det boolean ret = aList.equals(bList);
    }

    void testSetEquals9(@NonDet Set<@NonDet Integer> aList, @NonDet Set<@NonDet Integer> bList) {
        // :: error: (assignment.type.incompatible)
        @Det boolean ret = aList.equals(bList);
    }

    //Set equals List
    void testSetListEquals1(@Det Set<@Det Integer> aList, @Det List<@Det Integer> bList) {
        @Det boolean ret = aList.equals(bList);
    }

    void testSetListEqual2(@OrderNonDet Set<@Det Integer> aList, @Det List<@Det Integer> bList) {
        // :: error: (assignment.type.incompatible)
        @Det boolean ret = aList.equals(bList);
    }

    void testSetListEqual3(@NonDet Set<@Det Integer> aList, @Det List<@Det Integer> bList) {
        // :: error: (assignment.type.incompatible)
        @Det boolean ret = aList.equals(bList);
    }

    void testSetListEqual4(@Det Set<@Det Integer> aList, @OrderNonDet List<@Det Integer> bList) {
        // :: error: (assignment.type.incompatible)
        @Det boolean ret = aList.equals(bList);
    }

    void testSetListEqual5(
            @OrderNonDet Set<@Det Integer> aList, @OrderNonDet List<@Det Integer> bList) {
        // :: error: (assignment.type.incompatible)
        @Det boolean ret = aList.equals(bList);
    }

    void testSetListEqual6(@NonDet Set<@Det Integer> aList, @OrderNonDet List<@Det Integer> bList) {
        // :: error: (assignment.type.incompatible)
        @Det boolean ret = aList.equals(bList);
    }

    void testSetListEqual7(@Det Set<@Det Integer> aList, @NonDet List<@NonDet Integer> bList) {
        // :: error: (assignment.type.incompatible)
        @Det boolean ret = aList.equals(bList);
    }

    void testSetListEqual8(
            @OrderNonDet Set<@Det Integer> aList, @NonDet List<@NonDet Integer> bList) {
        // :: error: (assignment.type.incompatible)
        @Det boolean ret = aList.equals(bList);
    }

    void testSetListEqual9(
            @NonDet Set<@NonDet Integer> aList, @NonDet List<@NonDet Integer> bList) {
        // :: error: (assignment.type.incompatible)
        @Det boolean ret = aList.equals(bList);
    }

    void testSetSetEqualsNested(
            @OrderNonDet Set<@OrderNonDet Set<@Det Integer>> aList, @OrderNonDet Set<@OrderNonDet Set<@Det Integer>> bList) {
        @Det boolean ret = aList.equals(bList);
    }
    void testSetListEqualsNested(
            @OrderNonDet Set<@OrderNonDet Set<@Det Integer>> aList, @OrderNonDet Set<@OrderNonDet List<@Det Integer>> bList) {
        // :: error: (assignment.type.incompatible)
        @Det boolean ret = aList.equals(bList);
    }
}
