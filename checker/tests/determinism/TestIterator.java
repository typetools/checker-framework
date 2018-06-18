import java.util.*;
import org.checkerframework.checker.determinism.qual.*;

public class TestIterator {
    void testIterator1(@Det List<@Det String> lst) {
        @Det Iterator<@Det String> it = lst.iterator();
        while (it.hasNext()) {
            @Det String str = it.next();
        }
    }

    void testIterator2(@OrderNonDet List<@Det String> lst) {
        @OrderNonDet Iterator<@Det String> it = lst.iterator();
        while (it.hasNext()) {
            // :: error: (assignment.type.incompatible)
            @Det String str = it.next();
        }
    }
}
