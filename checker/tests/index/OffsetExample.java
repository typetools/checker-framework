import java.util.List;
import org.checkerframework.checker.index.qual.IndexFor;
import org.checkerframework.checker.index.qual.IndexOrHigh;
import org.checkerframework.common.value.qual.MinLen;

@SuppressWarnings("lowerbound")
public class OffsetExample {
    void example1(int @MinLen(2) [] a) {
        int j = 2;
        int x = a.length;
        int y = x - j;
        for (int i = 0; i < y; i++) {
            a[i + j] = 1;
        }
    }

    void example2(int @MinLen(2) [] a) {
        int j = 2;
        int x = a.length;
        int y = x - j;
        a[y] = 0;
        for (int i = 0; i < y; i++) {
            a[i + j] = 1;
            a[j + i] = 1;
            a[i + 0] = 1;
            a[i - 1] = 1;
            //::error: (array.access.unsafe.high)
            a[i + 2 + j] = 1;
        }
    }

    void example3(int @MinLen(2) [] a) {
        int j = 2;
        for (int i = 0; i < a.length - 2; i++) {
            a[i + j] = 1;
        }
    }

    void example4(int[] a, int offset) {
        int max_index = a.length - offset;
        for (int i = 0; i < max_index; i++) {
            a[i + offset] = 0;
        }
    }

    void example5(int[] a, int offset) {
        for (int i = 0; i < a.length - offset; i++) {
            a[i + offset] = 0;
        }
    }

    void test(@IndexFor("#3") int start, @IndexOrHigh("#3") int end, int[] a) {
        if (end > start) {
            // If start == 0, then end - start is end.  end might be equal to the length of a.  So the array access might be too high.
            //::error: (array.access.unsafe.high)
            a[end - start] = 0;
        }

        if (end > start) {
            a[end - start - 1] = 0;
        }
    }

    public static boolean isSubarray(Object[] a, Object[] sub, int a_offset) {
        int a_len = a.length - a_offset;
        int sub_len = sub.length;
        if (a_len < sub_len) {
            return false;
        }
        for (int i = 0; i < sub_len; i++) {
            if (sub[i] == a[a_offset + i]) {
                return false;
            }
        }
        return true;
    }

    public void test2(int[] a, List<Object> b) {
        int b_size = b.size();
        Object[] result = new Object[a.length + b_size];
        for (int i = 0; i < b_size; i++) {
            result[i + a.length] = b.get(i);
        }
    }
}
