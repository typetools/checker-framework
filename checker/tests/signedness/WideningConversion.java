public class WideningConversion {

    char c1;
    char c2;
    int i1;
    int i2;

    void compare() {
        boolean b;
        b = c1 > c2;
        b = c1 > i2;
        b = i1 > c2;
        b = i1 > i2;
    }
}
