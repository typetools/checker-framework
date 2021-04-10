package org.checkerframework.checker.mustcall;

import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedPrimitiveType;
import org.checkerframework.framework.type.typeannotator.TypeAnnotator;
import org.checkerframework.javacutil.TypesUtils;

/** Primitive types always have no must-call obligations. */
public class MustCallTypeAnnotator extends TypeAnnotator {

  /**
   * Create a type annotator.
   *
   * @param typeFactory the type factory
   */
  protected MustCallTypeAnnotator(MustCallAnnotatedTypeFactory typeFactory) {
    super(typeFactory);
  }

  @Override
  public Void visitDeclared(AnnotatedDeclaredType type, Void aVoid) {
    if (TypesUtils.isString(type.getUnderlyingType())) {
      type.replaceAnnotation(((MustCallAnnotatedTypeFactory) typeFactory).BOTTOM);
    }
    return super.visitDeclared(type, aVoid);
  }

  @Override
  public Void visitPrimitive(AnnotatedPrimitiveType type, Void aVoid) {
    type.replaceAnnotation(((MustCallAnnotatedTypeFactory) typeFactory).BOTTOM);
    return super.visitPrimitive(type, aVoid);
  }
}
