import java.util.*;
import org.checkerframework.checker.determinism.qual.*;

public class TestTreeSet {
    void testNewTreeSet(@OrderNonDet TreeSet<@Det Integer> treeSet) {
        Iterator it = treeSet.iterator();
        while (it.hasNext()) {
            System.out.println(it.next());
        }
    }

    void testNewTreeSet1(@NonDet TreeSet<@Det Integer> treeSet) {
        @NonDet Iterator<@Det Integer> it = treeSet.iterator();
        while (it.hasNext()) {
            // :: error: (argument.type.incompatible)
            System.out.println(it.next());
        }
    }

    void testTreeIterator(@OrderNonDet TreeSet<@OrderNonDet TreeSet> treeSet) {
        @Det NavigableSet<@Det TreeSet> nSet = treeSet.descendingSet();
    }

    void testTreeIterator1(
                    @OrderNonDet TreeSet<@OrderNonDet TreeSet<@OrderNonDet TreeSet<@Det Integer>>> treeSet) {
        @Det NavigableSet<@Det TreeSet<@Det TreeSet<@Det Integer>>> nSet = treeSet.descendingSet();
    }

    void testTreeSetEquals(@Det TreeSet<@Det Integer> t, @Det TreeSet<@Det Integer> q) {
        t.equals(q);
    }

    void testTreeSetEquals(@Det HashSet<@Det Integer> t, @Det HashSet<@Det Integer> q) {
        t.equals(q);
    }
}
