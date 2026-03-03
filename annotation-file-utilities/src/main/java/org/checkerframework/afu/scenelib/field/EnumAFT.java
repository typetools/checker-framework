package org.checkerframework.afu.scenelib.field;

import javax.lang.model.element.VariableElement;

/**
 * An {@link EnumAFT} is the type of an annotation field that can hold an constant from a certain
 * enumeration type.
 */
public final class EnumAFT extends ScalarAFT {

  // TODO: Is this a fully-qualified name or a binary name?
  /** The name of the enumeration type whose constants the annotation field can hold. */
  public final String typeName;

  /**
   * Constructs an {@link EnumAFT} for an annotation field that can hold constants of the
   * enumeration type with the given name.
   */
  public EnumAFT(String typeName) {
    this.typeName = typeName;
  }

  @Override
  public boolean isValidValue(Object o) {
    // return o instanceof Enum;
    return o instanceof String || o instanceof VariableElement;
  }

  @Override
  public String toString() {
    return "enum " + typeName;
  }

  @Override
  public void format(StringBuilder sb, Object o) {
    String fieldValue = o.toString();
    if (!fieldValue.contains(".")) {
      // If fieldValue is not qualified, prepend the typeName
      sb.append(typeName);
      sb.append(".");
    }
    sb.append(fieldValue);
  }

  @Override
  public <R, T> R accept(AFTVisitor<R, T> v, T arg) {
    return v.visitEnumAFT(this, arg);
  }
}
