import tests.signatureinference.qual.Top;
import tests.signatureinference.qual.*;
public class TestRelaxed {

    @Top
    private int field;

    void foo() {
        field = getTop();
    }

    void test() {
        expectsTop(field);
    }

    void expectsTop(@Top int i) {}

    @Top int getTop() {
        return (@Top int) 0;
    }

}
