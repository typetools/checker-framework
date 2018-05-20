//import java.util.ArrayList;
//import org.checkerframework.checker.determinism.qual.*;
//
//public class TestClone {
//    void cloneArrayList(@NonDet ArrayList<@Det Integer> ndArList) {
//        @NonDet Object arList = ndArList.clone();
//        @NonDet ArrayList<@Det Integer> arListClone =
//                (@NonDet ArrayList<@Det Integer>)arList;
//    }
//    void cloneArrayList1(@NonDet ArrayList<@NonDet Integer> ndArList) {
//        @NonDet Object arList = ndArList.clone();
//        @NonDet ArrayList<@Det Integer> arListClone =
//                (@NonDet ArrayList<@NonDet Integer>)arList;
//    }
//    void cloneArrayList2(@OrderNonDet ArrayList<@Det Integer> ondArList) {
//        @Det Object arList = ondArList.clone();
//    }
//}
