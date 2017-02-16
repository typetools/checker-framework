import org.checkerframework.checker.index.qual.LTLengthOf;

public final class Loops {

    public void test1(boolean flag, @LTLengthOf("#1") int offset, @LTLengthOf("#1") int offset2) {
        while (flag) {
            offset++;
            //:: error: (compound.assignment.type.incompatible)
            offset += 1;
            offset2 += offset;
        }
    }

    public void test2(boolean flag, int[] array) {
        int offset = array.length - 1;
        int offset2 = array.length - 1;

        while (flag) {
            offset++;
            offset2 += offset;
        }
        //:: error: (assignment.type.incompatible)
        @LTLengthOf("array") int x = offset;
        //:: error: (assignment.type.incompatible)
        @LTLengthOf("array") int y = offset2;
    }

    public void test3(boolean flag, @LTLengthOf("#1") int offset, @LTLengthOf("#1") int offset2) {
        while (flag) {
            offset--;
            offset2 -= offset;
        }
    }

    public void test4(boolean flag, @LTLengthOf("#1") int offset, @LTLengthOf("#1") int offset2) {
        while (flag) {
            offset++;
            //:: error: (compound.assignment.type.incompatible)
            offset += 1;
            offset2 += offset;
        }
    }

    public void test4(int[] src) {
        int patternLength = src.length;
        int[] optoSft = new int[patternLength];
        for (int i = patternLength; i > 0; i--) {}
    }
}
