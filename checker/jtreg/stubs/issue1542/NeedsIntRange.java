package issue1542;

public class NeedsIntRange {
    public static int range(boolean big) {
        if (big) {
            return 20000;
        } else {
            return 3;
        }
    }
}
