import java.util.ArrayList;
import org.checkerframework.checker.determinism.qual.*;

public class TestClone {
    void cloneArrayList(@NonDet ArrayList<@Det Integer> ndArList) {
        @NonDet Object arList = ndArList.clone();
    }

    void cloneArrayList1(@NonDet ArrayList<@NonDet Integer> ndArList) {
        // ::error: (assignment.type.incompatible)
        @Det Object arList = ndArList.clone();
    }

    void cloneArrayList2(@OrderNonDet ArrayList<@Det Integer> ondArList) {
        Object arList = ondArList.clone();
    }
}
