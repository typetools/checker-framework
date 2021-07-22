// This file may not be renamed; it has to have the same filename as ../issue3447/Test.java .
public class Test {
    public int test() {
        int a = 1, b = 2, c = 3;
        if (a > 0) {
            int d = a + c;
        } else {
            int e = a + b;
        }
        return a;
    }
}
