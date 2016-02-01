import tests.signatureinference.qual.Sibling2;
import tests.signatureinference.qual.Parent;
import tests.signatureinference.qual.*;
public class PublicFieldTest {
    public static @Parent int field1; // parent
    public static @Sibling2 int field2; // sib2

    public PublicFieldTest() {
        field1 = getSibling1();
    }

    void testPublicInference() {
        expectsSibling2(field2);
        expectsParent(field1);
        expectsParent(field2);
    }

    void expectsBottom(@SignatureInferenceBottom int t) {}
    void expectsSibling1(@Sibling1 int t) {}
    void expectsSibling2(@Sibling2 int t) {}
    void expectsTop(@Top int t) {}
    void expectsParent(@Parent int t) {}

    @Sibling1 int getSibling1() {
        return (@Sibling1 int) 0;
    }
}

class AnotherClass {

    @Sibling2
    int innerField;
    public AnotherClass() {
        PublicFieldTest.field1 = getSibling2();
        PublicFieldTest.field2 = getSibling2();
        innerField = getSibling2();
    }

    void innerFieldTest() {
        expectsSibling2(innerField);
    }

    @SignatureInferenceBottom int getBottom() {
        return (@SignatureInferenceBottom int) 0;
    }
    @Top int getTop() {
        return (@Top int) 0;
    }
    @Sibling2 int getSibling2() {
        return (@Sibling2 int) 0;
    }
    void expectsSibling2(@Sibling2 int t) {}
}
