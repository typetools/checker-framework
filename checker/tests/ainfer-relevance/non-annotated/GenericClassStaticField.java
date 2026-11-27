import org.checkerframework.checker.testchecker.ainfer.qual.AinferSibling1;

// Whole-program inference writes a postcondition about a static field of a generic class.  It must
// name the field as `GenericClassStaticField.charSequenceField`, without the class's type
// arguments:  the second (validation) pass of this test reports a parse error for an expression
// such as `GenericClassStaticField<T>.charSequenceField`.
public class GenericClassStaticField<T> {

  static CharSequence charSequenceField;

  static void assignField() {
    charSequenceField = getSibling1String();
  }

  static void useField() {
    // :: warning: [argument]
    expectsSibling1CharSequence(charSequenceField);
  }

  static void expectsSibling1CharSequence(@AinferSibling1 CharSequence cs) {}

  @SuppressWarnings("cast.unsafe") // This is how to obtain a @AinferSibling1 String.
  static @AinferSibling1 String getSibling1String() {
    return (@AinferSibling1 String) "";
  }
}
