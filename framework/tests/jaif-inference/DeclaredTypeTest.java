import tests.jaifinference.qual.*;
public class DeclaredTypeTest {
    // Case where the declared type is a supertype of the refined type.
    private @Top int privateDeclaredField;
    public @Top int publicDeclaredField;

    // The type of both privateDeclaredField and publicDeclaredField are
    // refined to @JaifBottom.
    void assignFieldsToBottom() {
        privateDeclaredField = getBottom();
        publicDeclaredField = getBottom();
    }

    // Testing the refinement above.
    void testFields() {
        expectsBottom(privateDeclaredField);
        expectsBottom(publicDeclaredField);
    }

    // Case where the declared type is a subtype of the refined type.
    private @JaifBottom int privateDeclaredField2;
    public @JaifBottom int publicDeclaredField2;

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

    void expectsBottom(@JaifBottom int t) {}
    void expectsTop(@Top int t) {}
    @JaifBottom int getBottom() {
        return (@JaifBottom int) 0;
    }
    @Top int getTop() {
        return (@Top int) 0;
    }
}
