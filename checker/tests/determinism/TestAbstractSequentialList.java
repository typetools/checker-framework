import java.util.*;
import org.checkerframework.checker.determinism.qual.*;

public class TestAbstractSequentialList {
    void test(@OrderNonDet AbstractSequentialList<@Det String> aList, @NonDet String a) {
        // :: error: (argument.type.incompatible)
        aList.add(a);
    }
}
