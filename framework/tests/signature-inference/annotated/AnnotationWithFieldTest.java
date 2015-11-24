import tests.signatureinference.qual.SiblingWithFields;
import tests.signatureinference.qual.DefaultType;
import tests.signatureinference.qual.*;
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

    @SiblingWithFields(value={"test","test2"}, value2="test3")
    String getSiblingWithFields() {
        //:: warning: (cast.unsafe) 
        return (@SiblingWithFields(value={"test", "test2"}, value2="test3") String) "";
    }

    @SiblingWithFields(value={}, value2="")
    String getSiblingWithFieldsEmpty() {
        //:: warning: (cast.unsafe) 
        return (@SiblingWithFields(value={}, value2="") String) "";
    }

}
