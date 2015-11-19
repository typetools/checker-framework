import tests.jaifinference.qual.SiblingWithFields;
import tests.jaifinference.qual.DefaultType;
import tests.jaifinference.qual.Parent;
import tests.jaifinference.qual.Sibling1;
import tests.jaifinference.qual.Sibling2;
import tests.jaifinference.qual.JaifBottom;
import tests.jaifinference.qual.*;
public class TopBottomTest {
    // The default type for fields is @DefaultType.
    @JaifBottom
    private int privateField;
    @JaifBottom
    public int publicField;

    // The type of privateField is refined to @JaifBottom
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

    void expectsBottom(@JaifBottom int t) {}
    @JaifBottom int getBottom() {
        return 0;
    }
}
