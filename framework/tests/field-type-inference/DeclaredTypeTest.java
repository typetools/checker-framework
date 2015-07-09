import tests.fieldtypeinference.qual.*;

public class DeclaredTypeTest {
    // Case where the declared type is a supertype of the refined type.
    private @Top int privateDeclaredField;
    public @Top int publicDeclaredField;

    // The type of privateDeclaredField is refined to @FieldTypeInferenceBottom
    // because of the first method call in the method below.
    void assignFieldsToBottom() {
        privateDeclaredField = getBottom();
        publicDeclaredField = getBottom();
    }

    // Testing the refinement above.
    void testFields() {
        expectsBottom(privateDeclaredField);
        // public fields are not refined.
        //:: error: (argument.type.incompatible)
        expectsBottom(publicDeclaredField);
    }

    // Case where the declared type is a subtype of the refined type.
    private @FieldTypeInferenceBottom int privateDeclaredField2;
    public @FieldTypeInferenceBottom int publicDeclaredField2;

    // The refinement cannot happen and an assignemnt type incompatible error occurs.
    void assignFieldsToTop() {
        //:: error: (assignment.type.incompatible)
        privateDeclaredField2 = getTop();
        //:: error: (assignment.type.incompatible)
        publicDeclaredField2 = getTop();
    }

    // Testing that the types above were not widden.
    void testFields2() {
        expectsBottom(privateDeclaredField2);
        expectsBottom(publicDeclaredField2);
    }

    void expectsBottom(@FieldTypeInferenceBottom int t) {}
    void expectsTop(@Top int t) {}
    @FieldTypeInferenceBottom int getBottom() {
        return 0;
    }
    @Top int getTop() {
        return 0;
    }
}
