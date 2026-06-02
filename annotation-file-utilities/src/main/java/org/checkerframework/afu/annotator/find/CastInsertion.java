package org.checkerframework.afu.annotator.find;

import org.checkerframework.afu.scenelib.type.Type;

/**
 * Specifies an insertion of a cast into a source file. Stores the type of cast to insert in
 * addition to the annotation and location.
 *
 * <p>In order to restrict the cast to only the specified expression, a cast insertion is of the
 * form:
 *
 * <pre>
 * ((<em>cast type</em>) (<em>original expression</em>))
 * </pre>
 *
 * This insertion inserts the cast type and parentheses that go before the original expression. A
 * {@link CloseParenthesisInsertion} must be used after the expression to close the parentheses left
 * open by this insertion.
 */
public class CastInsertion extends Insertion {

  /** The type to cast to. */
  private Type type;

  /** True if insertion is to take place on a bare array literal. */
  public boolean onArrayLiteral = false;

  /**
   * Creates a new CastInsertion.
   *
   * @param criteria where to insert the text
   * @param type the un-annotated type to cast to
   */
  public CastInsertion(Criteria criteria, Type type) {
    super(criteria, false);
    this.type = type;
  }

  /**
   * Gets the type for this insertion. It is assumed that the returned value will be modified to
   * update the type to be inserted.
   *
   * @return the type
   */
  public Type getType() {
    return type;
  }

  protected void setType(Type t) {
    type = t;
  }

  @Override
  protected String getText(boolean abbreviate) {
    String result =
        onArrayLiteral
            ? "((new " + typeToString(type, abbreviate) + " "
            : "((" + typeToString(type, abbreviate) + ") (";
    return result;
  }

  @Override
  protected boolean addLeadingSpace(boolean gotSeparateLine, int pos, char precedingChar) {
    // Don't add a leading space if this cast is on the index of an array access.
    return super.addLeadingSpace(gotSeparateLine, pos, precedingChar) && precedingChar != '[';
  }

  @Override
  protected boolean addTrailingSpace(boolean gotSeparateLine) {
    // Never add a trailing space after the first part of a cast insertion.
    return false;
  }

  public boolean isOnArrayLiteral() {
    return onArrayLiteral;
  }

  public void setOnArrayLiteral(boolean onArrayLiteral) {
    this.onArrayLiteral = onArrayLiteral;
  }

  @Override
  public Kind getKind() {
    return Kind.CAST;
  }
}
