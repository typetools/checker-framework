// @skip-test
import java.util.ArrayList;
import org.checkerframework.checker.determinism.qual.*;

class TestAliasing {
    void test(
            @NonDet ArrayList<@Det String> list1,
            @Det ArrayList<@Det String> list2,
            @NonDet int index,
            @Det String str) {
        list1 = list2;
        list1.add(index, str);
        //        @Det String out = list2.get(index);
    }
}
