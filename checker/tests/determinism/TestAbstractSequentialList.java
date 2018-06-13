import java.util.*;
import org.checkerframework.checker.determinism.qual.*;

public class TestAbstractSequentialList {
    void test(@OrderNonDet AbstractSequentialList<@Det String> aList, @NonDet String a) {
        aList.add(a);
    }
}
