import testlib.wholeprograminference.qual.*;

public class PublicFieldTest {
    public static int field1; // parent
    public static int field2; // sib2

    public PublicFieldTest() {
        field1 = getSibling1();
    }

    void testPublicInference() {
        // :: error: (argument.type.incompatible)
        expectsSibling2(field2);
        // :: error: (argument.type.incompatible)
        expectsParent(field1);
        // :: error: (argument.type.incompatible)
        expectsParent(field2);
    }

    void expectsBottom(@WholeProgramInferenceBottom int t) {}

    void expectsSibling1(@Sibling1 int t) {}

    void expectsSibling2(@Sibling2 int t) {}

    void expectsTop(@Top int t) {}

    void expectsParent(@Parent int t) {}

    @Sibling1 int getSibling1() {
        return (@Sibling1 int) 0;
    }
}

class AnotherClass {

    int innerField;

    public AnotherClass() {
        PublicFieldTest.field1 = getSibling2();
        PublicFieldTest.field2 = getSibling2();
        innerField = getSibling2();
    }

    void innerFieldTest() {
        // :: error: (argument.type.incompatible)
        expectsSibling2(innerField);
    }

    @WholeProgramInferenceBottom int getBottom() {
        return (@WholeProgramInferenceBottom int) 0;
    }

    @Top int getTop() {
        return (@Top int) 0;
    }

    @Sibling2 int getSibling2() {
        return (@Sibling2 int) 0;
    }

    void expectsSibling2(@Sibling2 int t) {}
}
