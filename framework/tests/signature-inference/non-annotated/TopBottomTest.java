import tests.signatureinference.qual.*;
public class TopBottomTest {
    // The default type for fields is @DefaultType.
    private int privateField;
    public int publicField;

    // The types of both fields are refined to @SignatureInferenceBottom
    // because of the method calls in the method below.
    void assignFieldsToBottom() {
        privateField = getBottom();
        publicField = getBottom();
    }

    // Testing the refinement above.
    void testFields() {
        //:: error: (argument.type.incompatible)
        expectsBottom(privateField);
        //:: error: (argument.type.incompatible)
        expectsBottom(publicField);
    }

    void expectsBottom(@SignatureInferenceBottom int t) {}
    @SignatureInferenceBottom int getBottom() {
        return 0;
    }
}
