import tests.jaifinference.qual.*;
public class AnnotationWithFieldTest {

    private String fields;

    private String emptyFields;

    void testAnnotationWithFields() {
        fields = getSiblingWithFields();
        expectsSiblingWithFields(fields);
        //:: error: (argument.type.incompatible)
        expectsSiblingWithFieldsWrong(fields);
    }

    void testAnnotationWithEmptyFields() {
        emptyFields = getSiblingWithFieldsEmpty();
        expectsSiblingWithEmptyFields(emptyFields);
        //:: error: (argument.type.incompatible)
        expectsSiblingWithFieldsWrong(emptyFields);
    }

    void expectsSiblingWithFields(@SiblingWithFields(value={"test", "test2"}, value2="test3") String s) {}
    void expectsSiblingWithEmptyFields(@SiblingWithFields(value={}, value2="") String s) {}
    void expectsSiblingWithFieldsWrong(@SiblingWithFields(value={"test"}, value2="test3") String s) {}

    @SiblingWithFields(value={"test", "test2"}, value2="test3") String getSiblingWithFields() {
        //:: error: (return.type.incompatible)
        return "";
    }

    @SiblingWithFields(value={}, value2="") String getSiblingWithFieldsEmpty() {
        //:: error: (return.type.incompatible)
        return "";
    }
}
