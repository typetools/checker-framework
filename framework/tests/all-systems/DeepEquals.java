public class DeepEquals {
    public static int deepEquals(Object o1) {
        if (o1 instanceof boolean[]) {
            return 1;
        }
        if (o1 instanceof byte[]) {
            return 2;
        }

        return 3;
    }
}
