import tests.fieldtypeinference.qual.*;

public class TopBottomTest {
    // The default type for fields is @Top.
    private int privateField;
    public int publicField;

    // The type of privateField is refined to @FieldTypeInferenceBottom
    // because of the first method call in the method below.
    void assignFieldsToBottom() {
        privateField = getBottom();
        publicField = getBottom();
    }

    // Testing the refinement above.
    void testFields() {
        expectsBottom(privateField);
        // public fields are not refined.
        //:: error: (argument.type.incompatible)
        expectsBottom(publicField);
    }

    void expectsBottom(@FieldTypeInferenceBottom int t) {}
    @FieldTypeInferenceBottom int getBottom() {
        return 0;
    }
}