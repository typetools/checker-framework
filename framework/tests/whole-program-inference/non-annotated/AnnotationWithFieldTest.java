import testlib.wholeprograminference.qual.*;

public class AnnotationWithFieldTest {

    private String fields;

    private String emptyFields;

    void testAnnotationWithFields() {
        fields = getSiblingWithFields();
        // :: error: (argument.type.incompatible)
        expectsSiblingWithFields(fields);
    }

    void testAnnotationWithEmptyFields() {
        emptyFields = getSiblingWithFieldsEmpty();
        // :: error: (argument.type.incompatible)
        expectsSiblingWithEmptyFields(emptyFields);
    }

    void expectsSiblingWithFields(
            @SiblingWithFields(
                            value = {"test", "test2"},
                            value2 = "test3")
                    String s) {}

    void expectsSiblingWithEmptyFields(
            @SiblingWithFields(
                            value = {},
                            value2 = "")
                    String s) {}

    String getSiblingWithFields() {
        // :: warning: (cast.unsafe)
        return (@SiblingWithFields(
                        value = {"test", "test2"},
                        value2 = "test3")
                String)
                "";
    }

    String getSiblingWithFieldsEmpty() {
        // :: warning: (cast.unsafe)
        return (@SiblingWithFields(
                        value = {},
                        value2 = "")
                String)
                "";
    }
}
