import tests.signatureinference.qual.*;
/**
 * This file contains expected errors that should exist even after the jaif type
 * inference occurs.
 */
public class ExpectedErrors {

    // Case where the declared type is a supertype of the refined type.
    private @Top int privateDeclaredField;
    public @Top int publicDeclaredField;

    // The type of both privateDeclaredField and publicDeclaredField are
    // not refined to @SignatureInferenceBottom.
    void assignFieldsToBottom() {
        privateDeclaredField = getBottom();
        publicDeclaredField = getBottom();
    }

    void testFields() {
        //:: error: (argument.type.incompatible)
        expectsBottom(privateDeclaredField);
        //:: error: (argument.type.incompatible)
        expectsBottom(publicDeclaredField);
    }

    // Case where the declared type is a subtype of the refined type.
    private @SignatureInferenceBottom int privateDeclaredField2;
    public @SignatureInferenceBottom int publicDeclaredField2;

    // The refinement cannot happen and an assignemnt type incompatible error occurs.
    void assignFieldsToTop() {
        //:: error: (assignment.type.incompatible)
        privateDeclaredField2 = getTop();
        //:: error: (assignment.type.incompatible)
        publicDeclaredField2 = getTop();
    }

    // No errors should be issued below:
    void assignFieldsToBot() {
        privateDeclaredField2 = getBottom();
        publicDeclaredField2 = getBottom();
    }

    // Testing that the types above were not widened.
    void testFields2() {
        expectsBottom(privateDeclaredField2);
        expectsBottom(publicDeclaredField2);
    }

    // LUB TEST
 // The default type for fields is @Top.
    private static int lubPrivateField;
    public static int lubPublicField;

    void assignFieldsToSibling1() {
        lubPrivateField = getSibling1();
        lubPublicField = getSibling1();
    }

    void assignFieldsToSibling2() {
        lubPrivateField = getSibling2();
        lubPublicField = getSibling2();
    }

    // TODO: Add support to static blocks. The static block below should replace
// the method above. Problem: It returns null when retrieving the class of the
// elements in the static block below.
//    static {
//        lubPrivateField = getSibling2();
//        lubPublicField = getSibling2();
//    }

    void testLUBFields1() {
        //:: error: (argument.type.incompatible)
        expectsSibling1(lubPrivateField);
        //:: error: (argument.type.incompatible)
        expectsSibling1(lubPublicField);
    }

    void testLUBFields2() {
        //:: error: (argument.type.incompatible)
        expectsSibling2(lubPrivateField);
        //:: error: (argument.type.incompatible)
        expectsSibling2(lubPublicField);
    }

    private static boolean bool = false;

    public static int lubTest() {
        if (bool) {
            return (@Sibling1 int) 0;
        } else {
            return (@Sibling2 int) 0;
        }
    }

    public @Sibling1 int getSibling1Wrong() {
        int x = lubTest();
        //:: error: (return.type.incompatible)
        return x;
    }

    public @Sibling2 int getSibling2Wrong() {
        int x = lubTest();
        //:: error: (return.type.incompatible)
        return x;
    }

    void expectsSibling1(@Sibling1 int t) {}
    void expectsSibling2(@Sibling2 int t) {}
    void expectsBottom(@SignatureInferenceBottom int t) {}
    void expectsTop(@Top int t) {}
    void expectsParent(@Parent int t) {}
    static @Sibling1 int getSibling1() {
        return 0;
    }
    static @Sibling2 int getSibling2() {
        return 0;
    }
    
    @SignatureInferenceBottom int getBottom() {
        return 0;
    }
    @Top int getTop() {
        return 0;
    }

    public class SuppressWarningsTest {
        // Tests that signature inference in a @SuppressWarnings block is ignored.
        private int i;
        private int i2;
        @SuppressWarnings("")
        public void suppressWarningsTest() {
            i = (@Sibling1 int) 0;
            i2 = getSibling1();
        }

        public void suppressWarningsTest2() {
            SuppressWarningsInner.i = (@Sibling1 int) 0;
            SuppressWarningsInner.i2 = getSibling1();
        }

        public void suppressWarningsValidation() {
            //:: error: (argument.type.incompatible)
            expectsSibling1(i);
            //:: error: (argument.type.incompatible)
            expectsSibling1(i2);
            //:: error: (argument.type.incompatible)
            expectsSibling1(SuppressWarningsInner.i);
            //:: error: (argument.type.incompatible)
            expectsSibling1(SuppressWarningsInner.i2);
            //:: error: (argument.type.incompatible)
            expectsSibling1(suppressWarningsMethodReturn());

            suppressWarningsMethodParams(getSibling1());
        }

        @SuppressWarnings("")
        public int suppressWarningsMethodReturn() {
            return getSibling1();
        }

        // It is problematic to automatically test signature inference for method
        // params when suppressing warnings.
        // Since we must use @SuppressWarnings() for the method,
        // we won't be able to catch any error inside the method body.
        // Verified manually that in the "annotated" folder param's type wasn't
        // updated.
        @SuppressWarnings("")
        public void suppressWarningsMethodParams(int param) {
        }
    }
}

@SuppressWarnings("")
class SuppressWarningsInner {
    public static int i;
    public static int i2;
}

