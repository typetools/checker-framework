package org.checkerframework.framework.type.typeannotator;

import javax.lang.model.element.Element;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.visitor.AnnotatedTypeScanner;

/**
 * {@link TypeAnnotator} is an abstract AnnotatedTypeScanner to be used with {@link
 * ListTypeAnnotator}.
 *
 * @see org.checkerframework.framework.type.typeannotator.ListTypeAnnotator
 * @see org.checkerframework.framework.type.typeannotator.PropagationTypeAnnotator
 * @see DefaultForTypeAnnotator
 */
public abstract class TypeAnnotator extends AnnotatedTypeScanner<Void, Void> {

  protected final AnnotatedTypeFactory typeFactory;

  protected TypeAnnotator(AnnotatedTypeFactory typeFactory) {
    this.typeFactory = typeFactory;
  }

  /**
   * {@inheritDoc}
   *
   * <p>If this method adds annotations to the type of method parameters, then {@link
   * org.checkerframework.framework.type.GenericAnnotatedTypeFactory#addComputedTypeAnnotations(Element,
   * AnnotatedTypeMirror)} should be overriden and the same annotations added to the type of
   * elements with kind {@link javax.lang.model.element.ElementKind#PARAMETER}. Likewise for return
   * types.
   */
  @Override
  public Void visitExecutable(AnnotatedExecutableType method, Void aVoid) {
    return super.visitExecutable(method, aVoid);
  }
}
