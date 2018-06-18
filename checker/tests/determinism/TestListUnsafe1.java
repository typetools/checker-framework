import java.util.ArrayList;
import java.util.List;
import org.checkerframework.checker.determinism.qual.*;

public class TestListUnsafe1 {
    void TestList() {
        @NonDet List<@Det Integer> lst = new @NonDet ArrayList<@Det Integer>();
        lst.add(20);
        for (int i = 0; i < 10; i++) {
            lst.add(i);
            @NonDet boolean z = lst.add(i);
            // :: error: (assignment.type.incompatible)
            @Det boolean r = z;
        }
        lst.clear();
    }

    void TestList1() {
        @OrderNonDet List<@Det Integer> lst = new @OrderNonDet ArrayList<@Det Integer>();
        @NonDet int rt = lst.remove(10);
    }
}
