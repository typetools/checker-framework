import java.util.*;
import org.checkerframework.checker.determinism.qual.*;

public class TestTreeSet {
    void testNewTreeSet(@OrderNonDet Set<@OrderNonDet Set<@Det Integer>> set) {
        @OrderNonDet TreeSet treeSet = new TreeSet(set);
        Iterator it = treeSet.iterator();
        while (it.hasNext()) {
            System.out.println(it.next());
        }
    }

    void testNewTreeSet1(@NonDet Set<@NonDet Integer> set) {
        @NonDet TreeSet<@Det Integer> treeSet = new TreeSet(set);
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
            @OrderNonDet
                    TreeSet<@OrderNonDet TreeSet<@OrderNonDet TreeSet<@Det Integer>>> treeSet) {
        @Det NavigableSet<@Det TreeSet<@Det TreeSet<@Det Integer>>> nSet = treeSet.descendingSet();
    }
}
