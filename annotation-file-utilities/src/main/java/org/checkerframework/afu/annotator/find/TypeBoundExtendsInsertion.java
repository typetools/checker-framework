package org.checkerframework.afu.annotator.find;

/** Specifies an insertion of an "extends @Annotation java.lang.Object" to a type bound. */
public class TypeBoundExtendsInsertion extends AnnotationInsertion {

  /**
   * Creates a new TypeBoundExtendsInsertion.
   *
   * @param text the text to insert
   * @param criteria where to insert the text
   * @param separateLine if true, insert the text on its own line
   */
  public TypeBoundExtendsInsertion(String text, Criteria criteria, boolean separateLine) {
    super(text, criteria, separateLine);
  }

  @Override
  protected String getText(boolean abbreviate) {
    return "extends java.lang." + super.getText(abbreviate) + " Object";
  }

  @Override
  protected boolean addTrailingSpace(boolean gotSeparateLine) {
    // Never add a trailing space for an extends insertion.
    return false;
  }
}
