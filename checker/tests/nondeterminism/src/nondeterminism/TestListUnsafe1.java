package nondeterminism;

import java.util.ArrayList;
import java.util.List;
import org.checkerframework.checker.nondeterminism.qual.*;

public class TestListUnsafe1 {
    void TestList() {
        @ValueNonDet List<@Det Integer> lst = new @ValueNonDet ArrayList<@Det Integer>();
        lst.add(20);
        for (int i = 0; i < 10; i++) {
            lst.add(i);
            @ValueNonDet boolean z = lst.add(i);
            @Det boolean r = z;
        }
    }
}
