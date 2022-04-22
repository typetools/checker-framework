class Test {
    public int test(int m) {
        int a = 2, b = 3;
        int x = 1;
        Integer y;
        if (a != b) {
            x = b >> a;
            y = new Integer(a - b);
        } else {
            y = b >> a;
            a = 0;
            test(a - b);
        }

        // test exceptional exit block
        int d;
        try {
            d = y / x;
        } catch (ArithmeticException e) {
            d = 10000000;
        }
        return d;
    }
}
