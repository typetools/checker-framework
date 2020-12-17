import org.checkerframework.checker.index.qual.Positive;

public class SimpleTransferSub {
    void test() {
        // shows a bug in the checker framework. I don't think we can get around this bit...
        int bs = 0;
        // :: error: (assignment.type.incompatible)
        @Positive int ds = bs--;
    }
}
// a comment
