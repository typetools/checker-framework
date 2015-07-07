import tests.fieldtypeinference.qual.*;

public class LUBTest {
    // The default type for fields is @Top.
    private static int privateField;
    public static int publicField;

    void assignFieldsToSibling1() {
        privateField = getSibling1();
        publicField = getSibling1();
    }

    static {
        privateField = getSibling2();
        publicField = getSibling2();
    }

    // LUB between @Sibling1 and @Sibling2 is @Parent, therefore the assignments
    // above refine the type of privateField to @Parent.
    void testFields() {
        expectsParent(privateField);
        // public fields are not refined.
        //:: error: (argument.type.incompatible)
        expectsParent(publicField);
    }

    void testFields2() {
        //:: error: (argument.type.incompatible)
        expectsSibling1(privateField);
        // public fields are not refined.
        //:: error: (argument.type.incompatible)
        expectsSibling1(publicField);
    }

    void testFields3() {
        //:: error: (argument.type.incompatible)
        expectsSibling2(privateField);
        // public fields are not refined.
        //:: error: (argument.type.incompatible)
        expectsSibling2(publicField);
    }

    void expectsSibling1(@Sibling1 int t) {}
    void expectsSibling2(@Sibling2 int t) {}
    void expectsParent(@Parent int t) {}
    static @Sibling1 int getSibling1() {
        return 0;
    }
    static @Sibling2 int getSibling2() {
        return 0;
    }
}