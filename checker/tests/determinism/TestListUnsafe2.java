import java.util.ArrayList;
import java.util.List;
import org.checkerframework.checker.determinism.qual.*;

public class TestListUnsafe2 {
    void TestList() {
        @OrderNonDet List<@Det Integer> lst = new @OrderNonDet ArrayList<@Det Integer>();
        @Det boolean a = lst.add(20);
    }
}
