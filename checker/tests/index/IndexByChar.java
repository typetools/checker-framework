import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.index.qual.Positive;

public class IndexByChar {

    // adapted from Guava
    public int m(char c) {
        int[] i = new int[128];
        if (c < 128) return i[c];
        else return -1;
    }

    public void basicTest(char c) {
        @NonNegative int x = c;
    }

    public void boxedTest(Character c) {
        @NonNegative int x = c.charValue();
        @NonNegative int y = c;
    }

    public void ensurePositiveNotReplaced(@Positive char c) {
        @Positive int x = c;
    }
}
