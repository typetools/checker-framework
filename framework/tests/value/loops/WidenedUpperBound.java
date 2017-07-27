import java.util.List;
import org.checkerframework.common.value.qual.IntRange;

// Because the analysis of loops isn't precise enough, the Value Checker issues
// warnings on this test case. So, suppress those warnings, but run the tests
// to make sure that dataflow reaches a fixed point.
// The expected errors in comments are the errors that should be issued if dataflow were precise enough.
@SuppressWarnings("value")
public class WidenedUpperBound {

    void increment() {
        int forIndex;
        for (forIndex = 0; forIndex < 4323; forIndex++) {
            @IntRange(from = 0, to = 4322) int x = forIndex;
        }
        ////::error: (assignment.type.incompatible)
        @IntRange(from = 0, to = 4322) int x = forIndex;
        @IntRange(from = 4323) int y = forIndex;

        int whileIndex = 0;
        while (whileIndex < 1234) {
            @IntRange(from = 0, to = 1233) int z = whileIndex;
            whileIndex++;
        }
        ////::error: (assignment.type.incompatible)
        @IntRange(from = 0, to = 1233) int a = whileIndex;
        @IntRange(from = 1234) int b = whileIndex;

        int doWhileIndex = 0;
        do {
            @IntRange(from = 0, to = 2344) int c = doWhileIndex;
            doWhileIndex++;
        } while (doWhileIndex < 2345);
        ////::error: (assignment.type.incompatible)
        @IntRange(from = 0, to = 2344) int d = doWhileIndex;
        @IntRange(from = 2345) int e = doWhileIndex;
    }

    void decrement() {
        int forIndex;
        for (forIndex = 4323; forIndex > 0; forIndex--) {
            @IntRange(from = 1, to = 4323) int x = forIndex;
        }
        ////::error: (assignment.type.incompatible)
        @IntRange(from = 1, to = 4323) int x = forIndex;
        @IntRange(to = 0) int y = forIndex;

        int whileIndex = 1234;
        while (whileIndex > 0) {
            @IntRange(from = 1, to = 1234) int z = whileIndex;
            whileIndex--;
        }
        ////::error: (assignment.type.incompatible)
        @IntRange(from = 1, to = 1234) int a = whileIndex;
        @IntRange(to = 0) int b = whileIndex;

        int doWhileIndex = 2344;
        do {
            @IntRange(from = 1, to = 2344) int c = doWhileIndex;
            doWhileIndex--;
        } while (doWhileIndex > 0);
        ////::error: (assignment.type.incompatible)
        @IntRange(from = 1, to = 2344) int d = doWhileIndex;
        @IntRange(to = 0) int e = doWhileIndex;
    }

    static void test1() {
        for (int i = 40; i > 0; i--) {
            @IntRange(from = 1, to = 40) int x = i;
        }
    }

    static void test1Explicit() {
        for (@IntRange(from = 1, to = 40) int i = 40; i > 0; i--) {
            @IntRange(from = 1, to = 40) int x = i;
        }
    }

    static void test2() {
        for (int i = 0; i < 18; i++) {
            @IntRange(from = 0, to = 17) int x = i;
        }
    }

    static void test2Explicit() {
        for (@IntRange(from = 0, to = 17) int i = 0; i < 18; i++) {
            @IntRange(from = 0, to = 17) int x = i;
        }
    }

    static void test3() {
        for (int i = 0; i < 18; i++) {
            @IntRange(from = 0, to = 17) int x = i;
        }
    }

    static void evenOdd(int param) {
        for (int i = 0; i < 12; i++) {
            if (i % 2 == 0) {
                @IntRange(from = 0, to = 11) int z = i;
            }
            @IntRange(from = 0, to = 11) int x = i;
        }

        for (int i = 0; i < 12; i++) {
            if (param == 0) {
                @IntRange(from = 0, to = 11) int z = i;
            }
            @IntRange(from = 0, to = 11) int x = i;
        }
    }

    static void evenOdd2(int param) {
        for (int i = 0; i < 300; i++) {
            if (i % 2 == 0) {
                @IntRange(from = 0, to = 299) int z = i;
            }
            @IntRange(from = 0, to = 299) int x = i;
        }

        for (int i = 0; i < 399; i++) {
            if (param == 0) {
                @IntRange(from = 0, to = 398) int z = i;
            }
            @IntRange(from = 0, to = 398) int x = i;
        }
    }

    void ifBlock(int param) {
        int x = 10;
        if (x < param) {
            x = param;
        }
        int z = 40;
        if (z < param) {
            param = 40;
        }
    }

    void doWhile() {
        int d = 0;
        do {
            @IntRange(from = 0, to = 399) int x = d;
            if (d % 2 == 0) {
                @IntRange(from = 0, to = 399) int y = d;
            }

            d++;
        } while (d < 399);

        @IntRange(from = 399) int z = d;
    }

    void doWhileMax() {
        int d = 0;
        do {
            @IntRange(from = 0) int x = d;
            if (d % 2 == 0) {
                @IntRange(from = 0) int y = d;
            }

            d++;
        } while (d < Integer.MAX_VALUE);

        @IntRange(from = 399) int z = d;
    }

    void whileLoop() {
        int i = 0;
        while (i < 399) {
            @IntRange(from = 0, to = 399) int x = i;
            if (i % 2 == 0) {
                @IntRange(from = 0, to = 399) int y = i;
            }

            i++;
        }

        @IntRange(from = 399) int z = i;
    }

    static void testMax() {
        for (int i = 0; i < Integer.MAX_VALUE; i++) {
            @IntRange(from = 0, to = Integer.MAX_VALUE - 1) int x = i;
        }
    }

    void exceptionLoop(List<Object> list) {
        int x = 0;
        for (short z = 0; z < list.size(); z++) {
            x = z;
            if (z == 100) {
                break;
            }
        }
        @IntRange(from = 0, to = 127) int result = x;
    }
}
