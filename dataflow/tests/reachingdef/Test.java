public class Test {
    public int test() {
        int a = 1, b = 2, c = 3;
        String x = "a", y = "b";
        if (a > 0) {
            int d = a + c;
        } else {
            int e = a + b;
        }
        b = 0;
        a = b;
        x += y;
        return a;
    }
}
