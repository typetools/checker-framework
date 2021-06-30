package org.checkerframework.checker.testchecker.ainfer;

import com.sun.source.tree.AnnotationTree;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeInfo;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import org.checkerframework.checker.testchecker.ainfer.qual.DefaultType;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;

/** Visitor for a simple type system to test whole-program inference using .jaif files. */
public class AinferTestVisitor extends BaseTypeVisitor<AinferTestAnnotatedTypeFactory> {

  public AinferTestVisitor(BaseTypeChecker checker) {
    super(checker);
  }

  @Override
  protected AinferTestAnnotatedTypeFactory createTypeFactory() {
    return new AinferTestAnnotatedTypeFactory(checker);
  }

  @Override
  public Void visitAnnotation(AnnotationTree node, Void p) {
    Element anno = TreeInfo.symbol((JCTree) node.getAnnotationType());
    if (anno.toString().equals(DefaultType.class.getName())) {
      checker.reportError(node, "annotation.not.allowed.in.src", anno.toString());
    }
    return super.visitAnnotation(node, p);
  }

  @Override
  protected void checkConstructorResult(
      AnnotatedExecutableType constructorType, ExecutableElement constructorElement) {
    // Skip this check.
  }
}
