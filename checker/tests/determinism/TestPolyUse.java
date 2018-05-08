import java.util.ArrayList;
import org.checkerframework.checker.determinism.qual.*;

public class TestPolyUse {
    void TestList(@Det ArrayList<@Det Integer> myDetList, @NonDet int rand) {
        // :: error: (argument.type.incompatible)
        myDetList.add(rand, 50);
    }

    void TestList1(@OrderNonDet ArrayList<@Det Integer> myList, @NonDet int rand) {
        // :: error: (argument.type.incompatible)
        myList.add(rand, 50);
    }

    void TestList2(@OrderNonDet ArrayList<@Det Integer> myList, @Det int rand) {
        myList.add(rand, 50);
    }
}
