import org.checkerframework.checker.index.qual.*;

public class ParserOffsetTest {

    public void subtraction1(String[] a, @IndexFor("#1") int i) {
        int length = a.length;
        if (i >= length - 1 || a[i + 1] == null) {
            // body is irrelevant
        }
    }

    public void addition1(String[] a, @IndexFor("#1") int i) {
        int length = a.length;
        if ((i + 1) >= length || a[i + 1] == null) {
            // body is irrelevant
        }
    }

    public void subtraction2(String[] a, @IndexFor("#1") int i) {
        if (i < a.length - 1) {
            @IndexFor("a") int j = i + 1;
        }
    }

    public void addition2(String[] a, @IndexFor("#1") int i) {
        if ((i + 1) < a.length) {
            @IndexFor("a") int j = i + 1;
        }
    }

    public void addition3(String[] a, @IndexFor("#1") int i) {
        if ((i + 5) < a.length) {
            @IndexFor("a") int j = i + 5;
        }
    }

    @SuppressWarnings("lowerbound")
    public void general_test(String[] a, @NonNegative int k) {
        if (k - 5 < a.length) {
            String s = a[k - 5];
            @IndexFor("a") int j = k - 5;
        }
    }
}
