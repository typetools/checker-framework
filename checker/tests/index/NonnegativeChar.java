import java.util.ArrayList;
import org.checkerframework.checker.index.qual.LowerBoundBottom;
import org.checkerframework.checker.index.qual.PolyLowerBound;

public class NonnegativeChar {
    void foreach(char[] array) {
        for (char value : array) ; // line 7
    }

    char constant() {
        return Character.MAX_VALUE; // line 11
    }

    char conversion(int i) {
        return (char) i; // line 15
    }

    public void takeList(ArrayList<Character> z) {}

    public void passList() {
        takeList(new ArrayList<Character>()); // line 20
    }

    static class CustomList extends ArrayList<Character> {}

    public void passCustomList() {
        takeList(new CustomList()); // line 25
    }

    public @LowerBoundBottom char bottomLB(@LowerBoundBottom char c) {
        return c; // line 29
    }

    public @PolyLowerBound char polyLB(@PolyLowerBound char c) {
        return c; // line 32
    }
}
