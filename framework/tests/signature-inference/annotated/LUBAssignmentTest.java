import tests.signatureinference.qual.SiblingWithFields;
import tests.signatureinference.qual.DefaultType;
import tests.signatureinference.qual.Sibling2;
import tests.signatureinference.qual.Sibling1;
import tests.signatureinference.qual.Parent;
import tests.signatureinference.qual.*;
public class LUBAssignmentTest {
    // The default type for fields is @DefaultType.
    private static @Parent int privateField;
    public static @Parent int publicField;

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
        expectsParent(publicField);
    }

    void expectsParent(@Parent int t) {}
    static @Sibling1 int getSibling1() {
        return 0;
    }
    static @Sibling2 int getSibling2() {
        return 0;
    }
}
