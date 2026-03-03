package org.checkerframework.afu.scenelib.field;

import java.util.Collection;
import org.checkerframework.afu.scenelib.AnnotationBuilder;
import org.checkerframework.checker.nullness.qual.Nullable;

/** An {@link ArrayAFT} represents an annotation field type that is an array. */
public final class ArrayAFT extends AnnotationFieldType {

  /**
   * The element type of the array, or {@code null} if it is unknown (see {@link
   * AnnotationBuilder#addEmptyArrayField}).
   */
  public final @Nullable ScalarAFT elementType;

  /**
   * Constructs a new {@link ArrayAFT} representing an array type with the given element type.
   * {@code elementType} may be {@code null} to indicate that the element type is unknown (see
   * {@link AnnotationBuilder#addEmptyArrayField}).
   *
   * @param elementType the element type of the array, or {@code null} if it is unknown
   */
  public ArrayAFT(@Nullable ScalarAFT elementType) {
    this.elementType = elementType;
  }

  @Override
  public boolean isValidValue(Object o) {
    if (!(o instanceof Collection)) {
      return false;
    }
    Collection<?> asCollection = (Collection<?>) o;
    if (elementType == null) {
      return asCollection.isEmpty();
    }
    for (Object elt : asCollection) {
      if (!elementType.isValidValue(elt)) {
        return false;
      }
    }
    return true;
  }

  @Override
  public String toString() {
    return (elementType == null ? "unknown" : elementType.toString()) + "[]";
  }

  @Override
  public void format(StringBuilder sb, Object o) {
    Collection<?> asCollection = (Collection<?>) o;
    int size = asCollection.size();
    if (size == 1) {
      Object elt = asCollection.iterator().next();
      elementType.format(sb, elt);
      return;
    }
    sb.append("{");
    boolean notfirst = false;
    for (Object elt : asCollection) {
      if (notfirst) {
        sb.append(", ");
      } else {
        notfirst = true;
      }
      elementType.format(sb, elt);
    }
    sb.append("}");
  }

  @Override
  public <R, T> R accept(AFTVisitor<R, T> v, T arg) {
    return v.visitArrayAFT(this, arg);
  }
}
