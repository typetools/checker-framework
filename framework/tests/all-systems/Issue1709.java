// Testcase for Issue 1709
// https://github.com/typetools/checker-framework/issues/1709

import java.util.Iterator;
import java.util.List;

public class Issue1709 {
    public static void m(final List<? super Integer> l) {
        Iterator<? super Integer> it = l.iterator();
    }
}
