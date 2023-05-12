package org.checkerframework.framework.type.typeannotator;

import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.framework.qual.RelevantJavaTypes;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;

/**
 * Adds annotations to types that are not relevant specified by the {@link RelevantJavaTypes} on a
 * checker.
 */
public class IrrelevantTypeAnnotator extends TypeAnnotator {

  /** Annotations to add. */
  private final Set<? extends AnnotationMirror> annotations;

  /**
   * Annotate every type with the annotationMirror except for those whose underlying Java type is
   * one of (or a subtype of) a class in relevantClasses. (Only adds annotationMirror if no
   * annotation in the hierarchy are already on the type.) If relevantClasses includes
   * Object[].class, then all arrays are considered relevant.
   *
   * @param typeFactory AnnotatedTypeFactory
   * @param annotations annotations to add
   */
  @SuppressWarnings("rawtypes")
  public IrrelevantTypeAnnotator(
      GenericAnnotatedTypeFactory typeFactory, Set<? extends AnnotationMirror> annotations) {
    super(typeFactory);
    this.annotations = annotations;
  }

  @Override
  protected Void scan(AnnotatedTypeMirror type, Void aVoid) {
    switch (type.getKind()) {
      case TYPEVAR:
      case WILDCARD:
      case EXECUTABLE:
      case INTERSECTION:
      case UNION:
      case NULL:
      case NONE:
      case PACKAGE:
      case VOID:
        return super.scan(type, aVoid);
      default:
        // go on
    }

    if (!((GenericAnnotatedTypeFactory) typeFactory).isRelevant(type)) {
      // System.out.printf("not relevant: %s%n", type);
      type.addMissingAnnotations(annotations);
      // System.out.printf("not relevant: %s (after adding missing)%n", type);
    }
    return super.scan(type, aVoid);
  }
}
