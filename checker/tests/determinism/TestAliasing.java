// @skip-test
import java.util.ArrayList;
import org.checkerframework.checker.determinism.qual.*;

class TestAliasing {
    @NonDet ArrayList<@Det String> list1 = new ArrayList<>();

    void mutate() {}

    void test(@Det ArrayList<@Det String> list2, @NonDet int index, @Det String str) {
        list1 = list2;
        mutate();
        list1.add(index, str);
    }
}
