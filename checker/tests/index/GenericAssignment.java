import java.util.List;
import org.checkerframework.checker.index.qual.GTENegativeOne;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.index.qual.Positive;
import org.checkerframework.common.value.qual.IntRange;

public class GenericAssignment {
    public void assignNonNegativeList(List<@NonNegative Integer> l) {
        List<@NonNegative Integer> i = l; // line 10
    }

    public void assignPositiveList(List<@Positive Integer> l) {
        List<@Positive Integer> i = l; // line 13
    }

    public void assignGTENOList(List<@GTENegativeOne Integer> l) {
        List<@GTENegativeOne Integer> i = l; // line 16
    }

    // Similar examples that work
    public void assignNonNegativeArrayOK(@NonNegative Integer[] l) {
        @NonNegative Integer[] i = l;
    }

    public void assignIntRangeListOK(List<@IntRange(from = 0) Integer> l) {
        List<@IntRange(from = 0) Integer> i = l;
    }
}
