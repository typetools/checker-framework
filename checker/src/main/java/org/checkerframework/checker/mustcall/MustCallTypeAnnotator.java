package org.checkerframework.checker.mustcall;

import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedPrimitiveType;
import org.checkerframework.framework.type.typeannotator.TypeAnnotator;

/** Primitive types always have no must-call obligations. */
public class MustCallTypeAnnotator extends TypeAnnotator {

  /**
   * Create a MustCallTypeAnnotator.
   *
   * @param typeFactory the type factory
   */
  protected MustCallTypeAnnotator(MustCallAnnotatedTypeFactory typeFactory) {
    super(typeFactory);
  }

  @Override
  public Void visitPrimitive(AnnotatedPrimitiveType type, Void aVoid) {
    type.replaceAnnotation(((MustCallAnnotatedTypeFactory) typeFactory).BOTTOM);
    return super.visitPrimitive(type, aVoid);
  }
}
