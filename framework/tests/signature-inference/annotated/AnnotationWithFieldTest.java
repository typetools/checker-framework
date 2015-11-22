import tests.jaifinference.qual.SiblingWithFields;
import tests.jaifinference.qual.DefaultType;
import tests.jaifinference.qual.*;
public class AnnotationWithFieldTest {

    @DefaultType
    private String fields;

    @DefaultType
    private String emptyFields;

    void testAnnotationWithFields() {
        fields = getSiblingWithFields();
        expectsSiblingWithFields(fields);
    }

    void testAnnotationWithEmptyFields() {
        emptyFields = getSiblingWithFieldsEmpty();
        expectsSiblingWithEmptyFields(emptyFields);
    }

    void expectsSiblingWithFields(@SiblingWithFields(value={"test", "test2"}, value2="test3") String s) {}
    void expectsSiblingWithEmptyFields(@SiblingWithFields(value={}, value2="") String s) {}

    @SiblingWithFields(value2="test3", value={"test","test2"})
    String getSiblingWithFields() {
        //:: warning: (cast.unsafe) 
        return (@SiblingWithFields(value={"test", "test2"}, value2="test3") String) "";
    }

    @SiblingWithFields(value2="", value={})
    String getSiblingWithFieldsEmpty() {
        //:: warning: (cast.unsafe) 
        return (@SiblingWithFields(value={}, value2="") String) "";
    }

}
