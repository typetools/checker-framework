import tests.signatureinference.qual.SiblingWithFields;
import tests.signatureinference.qual.DefaultType;
import tests.signatureinference.qual.Parent;
import tests.signatureinference.qual.Sibling1;
import tests.signatureinference.qual.Top;
import tests.signatureinference.qual.SignatureInferenceBottom;
import tests.signatureinference.qual.Sibling2;
import tests.signatureinference.qual.*;
public class TopBottomTest {
    // The default type for fields is @DefaultType.
    @SignatureInferenceBottom
    private int privateField;
    @SignatureInferenceBottom
    public int publicField;

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
