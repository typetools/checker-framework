import tests.signatureinference.qual.*;
public class TestRelaxed {

    private int field;

    void foo() {
        //:: error: (assignment.type.incompatible)
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
