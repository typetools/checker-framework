package org.checkerframework.checker.confidential;

import com.sun.source.tree.Tree;
import javax.lang.model.element.ExecutableElement;
import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;
import org.checkerframework.checker.confidential.qual.Confidential;
import org.checkerframework.checker.formatter.qual.FormatMethod;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;

/** Visitor for the {@link ConfidentialChecker}. */
public class ConfidentialVisitor extends BaseTypeVisitor<BaseAnnotatedTypeFactory> {

  /**
   * Creates a {@link ConfidentialVisitor}.
   *
   * @param checker the checker that uses this visitor
   */
  public ConfidentialVisitor(BaseTypeChecker checker) {
    super(checker);
  }

  /**
   * Don't check that the constructor result is top. Checking that the super() or this() call is a
   * subtype of the constructor result is sufficient.
   *
   * <p>{@inheritDoc}
   */
  @Override
  protected void checkConstructorResult(
      AnnotatedExecutableType constructorType, ExecutableElement constructorElement) {}

  @Override
  @FormatMethod
  protected boolean commonAssignmentCheck(
      AnnotatedTypeMirror varType,
      AnnotatedTypeMirror valueType,
      Tree valueTree,
      @CompilerMessageKey String errorKey,
      Object... extraArgs) {
    // Permit casting anything to @Confidential.
    if (varType.hasEffectiveAnnotation(Confidential.class)) {
      return true;
    }
    return super.commonAssignmentCheck(varType, valueType, valueTree, errorKey, extraArgs);
  }
}
