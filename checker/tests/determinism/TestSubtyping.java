import java.util.ArrayList;
import org.checkerframework.checker.determinism.qual.*;

public class TestSubtyping {
    void TestSub() {
        @NonDet ArrayList<@Det Integer> lst = new ArrayList<Integer>();
        @Det ArrayList<@Det Integer> cpy = new ArrayList<Integer>();
        lst = cpy;
    }

    void TestSubArr() {
        @Det Integer @NonDet [] lst = new Integer[10];
        @Det Integer @Det [] cpy = new Integer[20];
        lst = cpy;
        cpy = lst;
    }

    void TestCollection() {
        // :: error: (invalid.element.type)
        @Det ArrayList<@NonDet Integer> list = null;
    }
}
