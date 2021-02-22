import android.support.annotation.IntRange;

public class Alias {
    public void androidIntRange() {
        // :: error: (assignment.type.incompatible)
        @IntRange(from = 0, to = 10) int j = 13;
        // :: error: (assignment.type.incompatible)
        @IntRange(from = 0) int k = -1;
    }
}
