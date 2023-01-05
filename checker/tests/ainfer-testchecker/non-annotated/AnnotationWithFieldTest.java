import org.checkerframework.checker.testchecker.ainfer.qual.AinferSiblingWithFields;

public class AnnotationWithFieldTest {

  private String fields;

  private String emptyFields;

  void testAnnotationWithFields() {
    fields = getAinferSiblingWithFields();
    // :: warning: (argument)
    expectsAinferSiblingWithFields(fields);
  }

  void testAnnotationWithEmptyFields() {
    emptyFields = getAinferSiblingWithFieldsEmpty();
    // :: warning: (argument)
    expectsAinferSiblingWithEmptyFields(emptyFields);
  }

  void expectsAinferSiblingWithFields(
      @AinferSiblingWithFields(
              value = {"test", "test2"},
              value2 = "test3")
          String s) {}

  void expectsAinferSiblingWithEmptyFields(
      @AinferSiblingWithFields(
              value = {},
              value2 = "")
          String s) {}

  @SuppressWarnings("cast.unsafe")
  String getAinferSiblingWithFields() {
    return (@AinferSiblingWithFields(
            value = {"test", "test2"},
            value2 = "test3")
        String)
        "";
  }

  @SuppressWarnings("cast.unsafe")
  String getAinferSiblingWithFieldsEmpty() {
    return (@AinferSiblingWithFields(
            value = {},
            value2 = "")
        String)
        "";
  }
}
