import java.util.Arrays;
import org.checkerframework.checker.index.qual.*;

class SearchIndexTests {
    public void test(short[] a, short instant) {
        int i = Arrays.binarySearch(a, instant);
        @SearchIndex("a") int z = i;
        //:: error: (assignment.type.incompatible)
        @SearchIndex("a") int y = 7;
        @LTLengthOf("a") int x = i;
    }

    void test2(int[] a, @SearchIndex("#1") int xyz) {
        if (0 > xyz) {
            @NegativeIndexFor("a") int w = xyz;
            @NonNegative int y = ~xyz;
            @LTEqLengthOf("a") int z = ~xyz;
        }
    }

    void test3(int[] a, @SearchIndex("#1") int xyz) {
        if (-1 >= xyz) {
            @NegativeIndexFor("a") int w = xyz;
            @NonNegative int y = ~xyz;
            @LTEqLengthOf("a") int z = ~xyz;
        }
    }

    void subtyping1(
            @SearchIndex({"#3", "#4"}) int x, @NegativeIndexFor("#3") int y, int[] a, int[] b) {
        //:: error: (assignment.type.incompatible)
        @SearchIndex({"a", "b"}) int z = y;
        @SearchIndex("a") int w = y;
        @SearchIndex("b") int p = x;
        //:: error: (assignment.type.incompatible)
        @NegativeIndexFor({"a", "b"}) int q = x;
    }
}
