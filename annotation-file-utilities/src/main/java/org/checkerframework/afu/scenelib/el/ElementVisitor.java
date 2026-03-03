package org.checkerframework.afu.scenelib.el;

public interface ElementVisitor<R, T> {
  R visitAnnotationDef(AnnotationDef el, T arg);

  R visitBlock(ABlock el, T arg);

  R visitClass(AClass el, T arg);

  R visitDeclaration(ADeclaration el, T arg);

  R visitExpression(AExpression el, T arg);

  R visitField(AField el, T arg);

  R visitMethod(AMethod el, T arg);

  R visitTypeElement(ATypeElement el, T arg);

  R visitTypeElementWithType(ATypeElementWithType el, T arg);

  R visitElement(AElement el, T arg);
}
