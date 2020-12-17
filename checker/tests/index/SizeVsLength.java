// test case for issue 91: https://github.com/kelloggm/checker-framework/issues/91

import org.checkerframework.checker.index.qual.*;

public class SizeVsLength {

    public int[] getArray(@NonNegative int size) {
        int[] values = new int[size];
        for (int i = 0; i < size; i++) {
            values[i] = 22;
        }
        return values;
    }
}
