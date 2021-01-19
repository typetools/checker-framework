import org.checkerframework.checker.testchecker.wholeprograminference.qual.SiblingWithFields;

public class AnnotationWithFieldTest {

    private String fields;

    private String emptyFields;

    void testAnnotationWithFields() {
        fields = getSiblingWithFields();
        // :: warning: (argument.type.incompatible)
        expectsSiblingWithFields(fields);
    }

    void testAnnotationWithEmptyFields() {
        emptyFields = getSiblingWithFieldsEmpty();
        // :: warning: (argument.type.incompatible)
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

    @SuppressWarnings("cast.unsafe")
    String getSiblingWithFields() {
        return (@SiblingWithFields(
                        value = {"test", "test2"},
                        value2 = "test3")
                String)
                "";
    }

    @SuppressWarnings("cast.unsafe")
    String getSiblingWithFieldsEmpty() {
        return (@SiblingWithFields(
                        value = {},
                        value2 = "")
                String)
                "";
    }
}
