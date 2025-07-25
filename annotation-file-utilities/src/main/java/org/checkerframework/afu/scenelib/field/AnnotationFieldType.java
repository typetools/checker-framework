package org.checkerframework.afu.scenelib.field;

import java.util.Map;
import org.checkerframework.afu.scenelib.el.AnnotationDef;
import org.checkerframework.afu.scenelib.io.IndexFileWriter;
import org.checkerframework.afu.scenelib.util.EqualByStringRepresentation;

/**
 * An {@link AnnotationFieldType} represents a type that can be the type of an annotation field.
 * Each subclass represents one kind of type allowed by the Java language.
 */
public abstract class AnnotationFieldType extends EqualByStringRepresentation {

  /**
   * Returns the string representation of the type that would appear in an index file. Used by
   * {@link IndexFileWriter}.
   */
  @Override
  public abstract String toString();

  /**
   * Formats an annotation field value.
   *
   * @param o the value to format
   * @return the formatted annotation field value
   */
  @Deprecated // TEMPORARY
  public final String format(Object o) {
    StringBuilder sb = new StringBuilder();
    format(sb, o);
    return sb.toString();
  }

  /**
   * Formats an annotation field value.
   *
   * @param sb where to format the value to
   * @param o the value to format
   */
  public abstract void format(StringBuilder sb, Object o);

  /** Returns true if this value is valid for this AnnotationFieldType. */
  public abstract boolean isValidValue(Object o);

  // TODO: add a cache?
  public static AnnotationFieldType fromClass(Class<?> c, Map<String, AnnotationDef> adefs) {
    if (c.isAnnotation()) {
      @SuppressWarnings("unchecked")
      Class<? extends java.lang.annotation.Annotation> cAnno =
          (Class<? extends java.lang.annotation.Annotation>) c;
      return new AnnotationAFT(AnnotationDef.fromClass(cAnno, adefs));
    } else if (c.isArray()) {
      return new ArrayAFT((ScalarAFT) fromClass(c.getComponentType(), adefs));
    } else if (BasicAFT.bafts.containsKey(c)) {
      return BasicAFT.bafts.get(c);
    } else if (c == Class.class) {
      return ClassTokenAFT.ctaft;
    } else if (c.isEnum()) {
      return new EnumAFT(c.getName());
    } else {
      throw new Error("Unrecognized class: " + c);
    }
  }

  /**
   * Returns an {@link AnnotationFieldType} containing all the information from both arguments, or
   * {@code null} if the two arguments contradict each other.
   *
   * <p>Currently this just merges the {@link ArrayAFT#elementType} field, so that if both arguments
   * are {@link ArrayAFT}s, one of known element type and the other of unknown element type, an
   * {@link ArrayAFT} of the known element type is returned. Furthermore, if both arguments are
   * {@link AnnotationAFT}s, the sub-definitions might directly or indirectly contain {@link
   * ArrayAFT}s, so {@link AnnotationDef#unify} is called to unify the sub-definitions recursively.
   */
  public static final AnnotationFieldType unify(
      AnnotationFieldType aft1, AnnotationFieldType aft2) {
    if (aft1.equals(aft2)) {
      return aft1;
    } else if (aft1 instanceof ArrayAFT && aft2 instanceof ArrayAFT) {
      if (((ArrayAFT) aft1).elementType == null) {
        return aft2;
      } else if (((ArrayAFT) aft2).elementType == null) {
        return aft1;
      } else {
        return null;
      }
    } else if (aft1 instanceof AnnotationAFT && aft2 instanceof AnnotationAFT) {
      AnnotationDef ud =
          AnnotationDef.unify(
              ((AnnotationAFT) aft1).annotationDef, ((AnnotationAFT) aft2).annotationDef);
      if (ud == null) {
        return null;
      } else {
        return new AnnotationAFT(ud);
      }
    } else {
      return null;
    }
  }

  public abstract <R, T> R accept(AFTVisitor<R, T> v, T arg);
}
