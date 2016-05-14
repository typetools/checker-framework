import tests.wholeprograminference.qual.*;
public class LUBAssignmentTest {
    // The default type for fields is @DefaultType.
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
        //:: error: (argument.type.incompatible)
        expectsParent(privateField);
        //:: error: (argument.type.incompatible)
        expectsParent(publicField);
    }

    void expectsParent(@Parent int t) {}
    static @Sibling1 int getSibling1() {
        return 0;
    }
    static @Sibling2 int getSibling2() {
        return 0;
    }

    String lubTest2() {
        if (Math.random() > 0.5) {
            //:: warning: (cast.unsafe)
            @Sibling1 String s = (@Sibling1 String) "";
            return s;
        } else {
            //:: warning: (cast.unsafe)
            @Sibling2 String s = (@Sibling2 String) "";
            return s;
        }
    }

}
