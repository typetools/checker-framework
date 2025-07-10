package org.checkerframework.afu.annotator.find;

/**
 * This insertion adds two closing parentheses to close the unclosed parentheses left by a {@link
 * CastInsertion}. This should be inserted after the expression that's being casted.
 */
public class CloseParenthesisInsertion extends Insertion {

  public CloseParenthesisInsertion(Criteria criteria, boolean separateLine) {
    super(criteria, separateLine);
  }

  @Override
  protected String getText(boolean abbreviate) {
    return "))";
  }

  @Override
  protected boolean addLeadingSpace(boolean gotSeparateLine, int pos, char precedingChar) {
    // Never add a leading space when inserting closing parentheses.
    return false;
  }

  @Override
  protected boolean addTrailingSpace(boolean gotSeparateLine) {
    // Never add a trailing space when inserting closing parentheses.
    return false;
  }

  @Override
  public Kind getKind() {
    return Kind.CLOSE_PARENTHESIS;
  }
}
