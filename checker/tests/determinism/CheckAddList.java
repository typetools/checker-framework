import java.util.ArrayList;
import org.checkerframework.checker.determinism.qual.*;

public class CheckAddList {
    void addToList(@Det ArrayList<@Det Integer> lst, @NonDet int i) {
        lst.add(i);
    }

    void addToList1(@NonDet ArrayList<@NonDet Integer> lst, int i) {
        lst.add(i);
    }

    void addToList2(
            @NonDet ArrayList<@NonDet ArrayList<@Det Integer>> lst,
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
