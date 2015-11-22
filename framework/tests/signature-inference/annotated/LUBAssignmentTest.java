import tests.jaifinference.qual.SiblingWithFields;
import tests.jaifinference.qual.DefaultType;
import tests.jaifinference.qual.Parent;
import tests.jaifinference.qual.*;
public class LUBAssignmentTest {
    // The default type for fields is @DefaultType.
    @Parent
    private static int privateField;
    @Parent
    public static int publicField;

    void assignFieldsToSibling1() {
        privateField = getSibling1();
        publicField = getSibling1();
    }

    void assignFieldsToSibling2() {
        privateField = getSibling2();
        publicField = getSibling2();
    }

    // TODO: Add support to static blocks. The static block below should replace
// the method above. Problem: It returns null when retrieving the class of the
// elements in the static block below.
//    static {
//        privateField = getSibling2();
//        publicField = getSibling2();
//    }

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
