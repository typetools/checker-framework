package nondeterminism;

import java.util.ArrayList;
import org.checkerframework.checker.nondeterminism.qual.*;

public class CheckAddList {
    void addToList(@Det ArrayList<@Det Integer> lst, @ValueNonDet int i) {
        lst.add(i);
    }

    void addToList1(@ValueNonDet ArrayList<@ValueNonDet Integer> lst, int i) {
        lst.add(i);
    }

    void addToList2(
            @ValueNonDet ArrayList<@ValueNonDet ArrayList<@Det Integer>> lst,
            @OrderNonDet ArrayList<@Det Integer> i) {
        lst.add(i);
        lst.iterator();
    }

    void addToList3(
            @Det ArrayList<@Det ArrayList<@Det Integer>> lst,
            @OrderNonDet ArrayList<@Det ArrayList<Integer>> i) {
        lst.addAll(i);
    }

    void addToList4(@ValueNonDet ArrayList<@Det Integer> lst, @ValueNonDet int i) {
        lst.add(i);
        lst.iterator();
    }
}
