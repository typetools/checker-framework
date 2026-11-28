package org.checkerframework.checker.confidential;

import com.sun.source.tree.Tree;
import javax.lang.model.element.ExecutableElement;
import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;
import org.checkerframework.checker.confidential.qual.Confidential;
import org.checkerframework.checker.formatter.qual.FormatMethod;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;

/** Visitor for the {@link ConfidentialChecker}. */
public class ConfidentialVisitor extends BaseTypeVisitor<ConfidentialAnnotatedTypeFactory> {

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

  /**
   * Permits assigning anything to a {@code @Confidential} location: externally,
   * {@code @Confidential} behaves as though it were a supertype of {@code @NonConfidential}.
   *
   * <p>Only the primary qualifier is relaxed. The value type's type arguments are still checked,
   * because {@code @Confidential Box<@NonConfidential String>} and {@code @Confidential
   * Box<@Confidential String>} are not interchangeable.
   *
   * <p>{@inheritDoc}
   */
  @Override
  @FormatMethod
  protected boolean commonAssignmentCheck(
      AnnotatedTypeMirror varType,
      AnnotatedTypeMirror valueType,
      Tree valueTree,
      @CompilerMessageKey String errorKey,
      Object... extraArgs) {
    if (varType.hasPrimaryAnnotation(Confidential.class)) {
      AnnotatedTypeMirror relaxedValueType = valueType.deepCopy();
      relaxedValueType.replaceAnnotation(atypeFactory.CONFIDENTIAL);
      return super.commonAssignmentCheck(varType, relaxedValueType, valueTree, errorKey, extraArgs);
    }
    return super.commonAssignmentCheck(varType, valueType, valueTree, errorKey, extraArgs);
  }
}
