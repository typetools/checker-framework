import java.util.ArrayList;
import org.checkerframework.checker.determinism.qual.*;

public class CheckAddList {
    void addToList(@Det ArrayList<@Det Integer> lst, @NonDet int i) {
        // :: error: (argument.type.incompatible)
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
        // :: error: (argument.type.incompatible)
        lst.addAll(i);
    }

    void addToList4(@NonDet ArrayList<@Det Integer> lst, @NonDet int i) {
        // :: error: (argument.type.incompatible)
        lst.add(i);
        lst.iterator();
    }
}
