import org.checkerframework.checker.index.qual.LTLengthOf;

public final class Loops {
    public static boolean flag = false;

    public void test1(int[] a, @LTLengthOf("#1") int offset, @LTLengthOf("#1") int offset2) {
        while (flag) {
            // :: error: (unary.increment.type.incompatible)
            offset++;
            // :: error: (compound.assignment.type.incompatible)
            offset += 1;
            // :: error: (compound.assignment.type.incompatible)
            offset2 += offset;
        }
    }

    public void test2(int[] a, int[] array) {
        int offset = array.length - 1;
        int offset2 = array.length - 1;

        while (flag) {
            offset++;
            offset2 += offset;
        }
        // :: error: (assignment.type.incompatible)
        @LTLengthOf("array") int x = offset;
        // :: error: (assignment.type.incompatible)
        @LTLengthOf("array") int y = offset2;
    }

    public void test3(int[] a, @LTLengthOf("#1") int offset, @LTLengthOf("#1") int offset2) {
        while (flag) {
            offset--;
            // :: error: (compound.assignment.type.incompatible)
            offset2 -= offset;
        }
    }

    public void test4(int[] a, @LTLengthOf("#1") int offset, @LTLengthOf("#1") int offset2) {
        while (flag) {
            // :: error: (unary.increment.type.incompatible)
            offset++;
            // :: error: (compound.assignment.type.incompatible)
            offset += 1;
            // :: error: (compound.assignment.type.incompatible)
            offset2 += offset;
        }
    }

    public void test4(int[] src) {
        int patternLength = src.length;
        int[] optoSft = new int[patternLength];
        for (int i = patternLength; i > 0; i--) {}
    }

    public void test5(
            int[] a,
            @LTLengthOf(value = "#1", offset = "-1000") int offset,
            @LTLengthOf("#1") int offset2) {
        int otherOffset = offset;
        while (flag) {
            otherOffset += 1;
            // :: error: (unary.increment.type.incompatible)
            offset++;
            // :: error: (compound.assignment.type.incompatible)
            offset += 1;
            // :: error: (compound.assignment.type.incompatible)
            offset2 += offset;
        }
        // :: error: (assignment.type.incompatible)
        @LTLengthOf(value = "#1", offset = "-1000") int x = otherOffset;
    }
}
