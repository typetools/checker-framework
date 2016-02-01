import tests.signatureinference.qual.SignatureInferenceBottom;
import tests.signatureinference.qual.*;
public class TopBottomTest {
    // The default type for fields is @DefaultType.
    private @SignatureInferenceBottom int privateField;
    public @SignatureInferenceBottom int publicField;

    // The type of privateField is refined to @SignatureInferenceBottom
    // because of the first method call in the method below.
    void assignFieldsToBottom() {
        privateField = getBottom();
        publicField = getBottom();
    }

    // Testing the refinement above.
    void testFields() {
        expectsBottom(privateField);
        expectsBottom(publicField);
    }

    void expectsBottom(@SignatureInferenceBottom int t) {}
    @SignatureInferenceBottom int getBottom() {
        return 0;
    }
}
