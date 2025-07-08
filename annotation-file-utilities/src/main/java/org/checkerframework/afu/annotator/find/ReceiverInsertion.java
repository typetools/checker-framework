package org.checkerframework.afu.annotator.find;

import java.util.List;
import org.checkerframework.afu.scenelib.type.DeclaredType;

/**
 * An insertion for a method receiver. This supports inserting an annotation on an existing receiver
 * and creating a new (annotated) receiver if none are present.
 */
public class ReceiverInsertion extends TypedInsertion {
  /**
   * If true a comma will be added at the end of the insertion (only if also inserting the
   * receiver).
   */
  private boolean addComma;

  /** If true, {@code this} will be qualified with the name of the superclass. */
  private boolean qualifyThis;

  /**
   * Construct a ReceiverInsertion.
   *
   * <p>If the receiver parameter already exists in the method declaration, then pass a DeclaredType
   * whose name is the empty String. This will only insert an annotation on the existing receiver.
   *
   * <p>To insert the annotation and the receiver (for example, {@code @Anno Type this}) the
   * DeclaredType's name should be set to the type to insert. This can either be done before calling
   * this constructor, or by modifying the return value of {@link #getType()}.
   *
   * <p>A comma will not be added to the end of the receiver. In the case that there is a parameter
   * following the inserted receiver pass {@code true} to {@link #setAddComma(boolean)} to add a
   * comma to the end of the receiver.
   *
   * @param type the type to use when inserting the receiver
   * @param criteria where to insert the text
   * @param innerTypeInsertions the inner types to go on this receiver
   */
  public ReceiverInsertion(
      DeclaredType type, Criteria criteria, List<Insertion> innerTypeInsertions) {
    super(type, criteria, innerTypeInsertions);
    addComma = false;
    qualifyThis = false;
  }

  /**
   * If {@code true} a comma will be added at the end of the receiver. This will only happen if a
   * receiver is inserted (see {@link #ReceiverInsertion(DeclaredType, Criteria, List)} for a
   * description of when a receiver is inserted). This is useful if the method already has one or
   * more parameters.
   */
  public void setAddComma(boolean addComma) {
    this.addComma = addComma;
  }

  /**
   * If {@code true}, qualify {@code this} with the name of the superclass. This will only happen if
   * a receiver is inserted (see {@link #ReceiverInsertion(DeclaredType, Criteria, List)} for a
   * description of when a receiver is inserted). This is useful for inner class constructors.
   */
  public void setQualifyType(boolean qualifyThis) {
    this.qualifyThis = qualifyThis;
  }

  @Override
  protected String getText(boolean abbreviate) {
    if (annotationsOnly) {
      StringBuilder b = new StringBuilder();
      List<String> annotations = type.getAnnotations();
      if (annotations.isEmpty()) {
        return "";
      }
      for (String a : annotations) {
        b.append(a);
        b.append(' ');
      }
      return new AnnotationInsertion(b.toString(), getCriteria(), isSeparateLine())
          .getText(abbreviate);
    } else {
      DeclaredType baseType = getBaseType();
      String result = typeToString(type, abbreviate);
      if (!baseType.getName().isEmpty()) {
        result += " ";
        if (qualifyThis) {
          for (DeclaredType t = baseType; t != null; t = t.getInnerType()) {
            result += t.getName() + ".";
          }
        }
        result += "this";
        if (addComma) {
          result += ",";
        }
      }
      return result;
    }
  }

  @Override
  protected boolean addLeadingSpace(boolean gotSeparateLine, int pos, char precedingChar) {
    if (precedingChar == '.' && getBaseType().getName().isEmpty()) {
      // If only the annotation is being inserted then don't insert a
      // space if it's immediately after a '.'
      return false;
    }
    return super.addLeadingSpace(gotSeparateLine, pos, precedingChar);
  }

  @Override
  protected boolean addTrailingSpace(boolean gotSeparateLine) {
    // If the type is not already in the source and the receiver is the only
    // parameter, don't add a trailing space.
    if (!getBaseType().getName().isEmpty() && !addComma) {
      return false;
    }
    return super.addTrailingSpace(gotSeparateLine);
  }

  @Override
  public Kind getKind() {
    return Kind.RECEIVER;
  }
}
