import org.checkerframework.checker.index.qual.GTENegativeOne;
import org.checkerframework.checker.index.qual.Positive;

class MinMax {
    // They call me a power gamer. I stole the test cases from issue 26.
    @Positive int mathmax() {
        return Math.max(-15, 2);
    }

    @GTENegativeOne int mathmin() {
        return Math.min(-1, 2);
    }
}
