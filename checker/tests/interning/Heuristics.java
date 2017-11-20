import java.util.Comparator;

public class Heuristics implements Comparable<Heuristics> {

    public static final class MyComparator implements Comparator<String> {
        // Using == is OK if it's the first statement in the compare method,
        // it's comparing the arguments, and the return value is 0.
        public int compare(String s1, String s2) {
            if (s1 == s2) {
                return 0;
            }
            return String.CASE_INSENSITIVE_ORDER.compare(s1, s2);
        }
    }

    @Override
    @org.checkerframework.dataflow.qual.Pure
    public boolean equals(Object o) {
        // Using == is OK if it's the first statement in the equals method
        // and it compares "this" against the argument.
        if (this == o) return true;
        // Not the first statement in the method.
        // :: error: (not.interned)
        if (o == this) return true;
        return false;
    }

    @Override
    @org.checkerframework.dataflow.qual.Pure
    public int compareTo(Heuristics o) {
        // Using == is OK if it's the first statement in the equals method
        // and it compares "this" against the argument.

        if (o == this) return 0;
        // Not the first statement in the method.
        // :: error: (not.interned)
        if (this == o) return 0;
        return 0;
    }

    public boolean optimizeEqualsClient(Object a, Object b, Object[] arr) {
        // Using == is OK if it's the left-hand side of an || whose right-hand
        // side is a call to equals with the same arguments.
        if (a == b || a.equals(b)) {
            System.out.println("one");
        }
        if (a == b || b.equals(a)) {
            System.out.println("two");
        }

        boolean c = (a == b || a.equals(b));
        c = (a == b || b.equals(a));

        boolean d = (a == b) || (a != null ? a.equals(b) : false);

        boolean e = (a == b || (a != null && a.equals(b)));

        boolean f = (arr[0] == a || arr[0].equals(a));

        return (a == b || a.equals(b));
    }

    public <T extends Comparable<T>> boolean optimizeCompareToClient(T a, T b) {
        // Using == is OK if it's the left-hand side of an || whose right-hand
        // side is a call to compareTo with the same arguments.
        if (a == b || a.compareTo(b) == 0) {
            System.out.println("one");
        }
        if (a == b || b.compareTo(a) == 0) {
            System.out.println("two");
        }

        boolean c = (a == b || a.compareTo(b) == 0);
        c = (a == b || a.compareTo(b) == 0);

        return (a == b || a.compareTo(b) == 0);
    }
}
