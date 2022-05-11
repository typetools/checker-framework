class Test {
    Test(int x) {}

    public int test(int m) {
        int a = 2, b = 3, x = 1, y;
        if (a != b) {
            x = b >> a;
            new Test(a - b); // test object creation
            y = a + b;
        } else {
            y = b >> a;
            a = 0;
            test(a - b); // test method invocation
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
