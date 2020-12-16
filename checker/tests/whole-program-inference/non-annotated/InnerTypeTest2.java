public class InnerTypeTest2 {
    public static int[] min_max(int[] a) {
        if (a.length == 0) {
            return null;
        }
        int result_min = a[0];
        int result_max = a[0];
        return new int[] {result_min, result_max};
    }
}
