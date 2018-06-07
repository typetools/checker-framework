import java.util.*;
import org.checkerframework.checker.determinism.qual.*;

public class TestListEquals {
    void TestList(@NonDet ArrayList<@NonDet Integer> inList) {
        @NonDet ArrayList<@NonDet Integer> myList = inList;
        // :: error: (assignment.type.incompatible)
        @Det boolean isEqual = myList.equals(inList);
    }

    void TestList1(@Det ArrayList<@Det Integer> inList) {
        @Det ArrayList<@Det Integer> myList = inList;
        @Det boolean isEqual = myList.equals(inList);
        // :: error: (argument.type.incompatible)
        System.out.println(myList.hashCode());
    }

    void TestList2(
            @OrderNonDet ArrayList<@Det Integer> myList,
            @OrderNonDet ArrayList<@Det Integer> inList) {
        myList.equals(inList);
    }

    void TestList3(@Det List<@Det Integer> inList) {
        @Det List<@Det Integer> myList = inList;
        // :: error: (argument.type.incompatible)
        System.out.println(myList.hashCode());
    }

    void TestList4(@Det ArrayList<@Det Integer> inList) {
        @Det ArrayList<@Det Integer> myList = inList;
        // :: error: (argument.type.incompatible)
        System.out.println(myList.hashCode());
        myList.equals(inList);
    }

    void TestList5(@NonDet ArrayList<@NonDet Integer> inList, @Det ArrayList<@Det Integer> myList) {
        // :: error: (argument.type.incompatible)
        @Det boolean isEqual = myList.equals(inList);
    }

    void TestList6(
            @OrderNonDet ArrayList<@Det Integer> inList, @Det ArrayList<@Det Integer> myList) {
        @NonDet boolean isEqual = inList.equals(myList);
    }

    void TestList7(
            @OrderNonDet ArrayList<@Det Integer> inList,
            @OrderNonDet ArrayList<@Det Integer> myList) {
        // :: error: (assignment.type.incompatible)
        @Det boolean isEqual = inList.equals(myList);
    }
}
