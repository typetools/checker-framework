import java.util.ArrayList;
import org.checkerframework.checker.index.qual.GTENegativeOne;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.index.qual.Positive;

// @skip-test until we bring list support back

public class ListSupportLBC {

    void testGet() {

        List<Integer> list = new ArrayList<>();
        int i = -1;
        int j = 0;

        // try and use a negative to get, should fail
        // :: error: (argument.type.incompatible)
        Integer m = list.get(i);

        // try and use a nonnegative, should work
        Integer l = list.get(j);
    }

    void testArrayListGet() {

        ArrayList<Integer> list = new ArrayList<>();
        int i = -1;
        int j = 0;

        // try and use a negative to get, should fail
        // :: error: (argument.type.incompatible)
        Integer m = list.get(i);

        // try and use a nonnegative, should work
        Integer l = list.get(j);
    }

    void testSet() {
        List<Integer> list = new ArrayList<>();
        int i = -1;
        int j = 0;

        // try and use a negative to get, should fail
        // :: error: (argument.type.incompatible)
        Integer m = list.set(i, 34);

        // try and use a nonnegative, should work
        Integer l = list.set(j, 34);
    }

    void testIndexOf() {
        List<Integer> list = new ArrayList<>();
        @GTENegativeOne int a = list.indexOf(1);

        // :: error: (assignment.type.incompatible)
        @NonNegative int n = a;

        @GTENegativeOne int b = list.lastIndexOf(1);

        // :: error: (assignment.type.incompatible)
        @NonNegative int m = b;
    }

    void testSize() {
        List<Integer> list = new ArrayList<>();
        @NonNegative int s = list.size();

        // :: error: (assignment.type.incompatible)
        @Positive int r = s;
    }

    void testSublist() {
        List<Integer> list = new ArrayList<>();
        int i = -1;
        int j = 0;

        // :: error: (argument.type.incompatible)
        List<Integer> k = list.subList(i, i);

        // :: error: (argument.type.incompatible)
        List<Integer> a = list.subList(i, j);

        // :: error: (argument.type.incompatible)
        List<Integer> b = list.subList(j, i);

        // should work since both are nonnegative
        List<Integer> c = list.subList(j, j);
    }
}
