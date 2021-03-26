public class ComputeConst {

    public static int hash(long l) {
        // If possible, use the value itself.
        if (l >= Integer.MIN_VALUE && l <= Integer.MAX_VALUE) {
            return (int) l;
        }

        return Long.hashCode(l);
    }
}
