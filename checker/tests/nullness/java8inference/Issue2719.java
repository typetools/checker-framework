import static java.util.Arrays.asList;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;

public class Issue2719 {
    public static void main(String[] args) {
        List<Integer> iList = asList(0);
        List<@Nullable Integer> jList = asList((Integer) null);
        // TODO:: error:  (assignment.type.incompatible)
        List<List<Integer>> both = passThrough(asList(iList, jList));
        System.out.println(both.get(1).get(0).intValue());
    }

    static <T> T passThrough(T object) {
        return object;
    }
}
