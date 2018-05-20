//import java.util.ArrayList;
//import org.checkerframework.checker.determinism.qual.*;
//
//public class TestPolyConstructor {
//    void createArrayList(@NonDet int i) {
//        @Det ArrayList<@Det Integer> arList = new ArrayList<Integer>(i);
//    }
//    void createArrayList1(@NonDet ArrayList<@NonDet Integer> c) {
//        @Det ArrayList<@Det Integer> arList = new ArrayList<Integer>(c);
//    }
//    void trimArrayList(@Det ArrayList<@Det String> arList){
//        arList.trimToSize();
//    }
//}
