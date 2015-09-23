import tests.jaifinference.qual.*;
public class FieldInferenceTest {

    private @Sibling1 int annotatedField = 0;
    public int refinedField = 0;
    public int notRefinedField = 0;

    public void annotateField() {
        refinedField = annotatedField;
    }

    public @Sibling1 int getSiblingRefined() {
        return refinedField;
    }

    public @Sibling1 int getSiblingNotRefined() {
        //:: error: (return.type.incompatible)
        return notRefinedField;
    }
}
