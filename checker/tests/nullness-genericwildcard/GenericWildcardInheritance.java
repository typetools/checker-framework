// Test case for issue #511: https://github.com/typetools/checker-framework/issues/511
// If GwiParent is compiled together with this file, no error occurs.
// If GwiParent is read from bytecode, the error occurs.

public class GenericWildcardInheritance extends GwiParent {
  @Override
  public void syntaxError(Recognizer<?> recognizer) {}
}
