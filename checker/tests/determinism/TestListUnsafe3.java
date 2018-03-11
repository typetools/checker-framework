import java.util.ArrayList;
import org.checkerframework.checker.determinism.qual.*;

public class TestListUnsafe3 {
    void TestList() {
        @NonDet ArrayList<@Det Integer> lst = new @NonDet ArrayList<@Det Integer>();
        // :: error: (assignment.type.incompatible)
        @Det ArrayList<@Det Integer> cpy = lst;
        lst.add(20);
    }
}
