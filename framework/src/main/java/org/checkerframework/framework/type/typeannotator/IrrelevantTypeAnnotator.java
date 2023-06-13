package org.checkerframework.framework.type.typeannotator;

import javax.lang.model.type.TypeMirror;
import org.checkerframework.framework.qual.RelevantJavaTypes;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;
import org.checkerframework.javacutil.BugInCF;

/**
 * Adds annotations to types that are not relevant specified by the {@link RelevantJavaTypes} on a
 * checker.
 */
public class IrrelevantTypeAnnotator extends TypeAnnotator {

  /**
   * Annotate every type except for those whose underlying Java type is one of (or a subtype or
   * supertype of) a class in relevantClasses. (Only adds annotationMirror if no annotation in the
   * hierarchy are already on the type.) If relevantClasses includes Object[].class, then all arrays
   * are considered relevant.
   *
   * @param atypeFactory a GenericAnnotatedTypeFactory
   */
  @SuppressWarnings("rawtypes")
  public IrrelevantTypeAnnotator(GenericAnnotatedTypeFactory atypeFactory) {
    super(atypeFactory);
  }

  @Override
  protected Void scan(AnnotatedTypeMirror type, Void aVoid) {
    GenericAnnotatedTypeFactory<?, ?, ?, ?> gatf = (GenericAnnotatedTypeFactory) atypeFactory;

    TypeMirror tm = type.getUnderlyingType();
    if (shouldAddPrimaryAnnotation(tm) && !gatf.isRelevant(tm)) {
      type.addMissingAnnotations(gatf.annotationsForIrrelevantJavaType(type.getUnderlyingType()));
    }

    return super.scan(type, aVoid);
  }

  /**
   * Returns true if IrrelevantTypeAnnotator should add a primary annotation.
   *
   * @param tm a type mirror
   * @return true if IrrelevantTypeAnnotator should add a primary annotation
   */
  boolean shouldAddPrimaryAnnotation(TypeMirror tm) {
    switch (tm.getKind()) {
      case BOOLEAN:
      case BYTE:
      case CHAR:
      case DOUBLE:
      case FLOAT:
      case INT:
      case LONG:
      case SHORT:
        return true;

      case DECLARED:
        return true;

      case ARRAY:
        return true;
      case TYPEVAR:
      case WILDCARD:
        return false;

      case ERROR:
      case EXECUTABLE:
      case INTERSECTION:
      case MODULE:
      case NONE:
      case NULL:
      case OTHER:
      case PACKAGE:
      case UNION:
      case VOID:
        return false;

      default:
        throw new BugInCF("Unknown type kind %s for %s", tm.getKind(), tm);
    }
  }
}
