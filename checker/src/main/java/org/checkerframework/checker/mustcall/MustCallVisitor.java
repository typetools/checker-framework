package org.checkerframework.checker.mustcall;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.Tree;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;
import org.checkerframework.checker.mustcall.qual.MustCallAlias;
import org.checkerframework.checker.mustcall.qual.NotOwning;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TreePathUtil;
import org.checkerframework.javacutil.TreeUtils;

/**
 * The visitor for the Must Call checker. This visitor is similar to BaseTypeVisitor, but overrides
 * methods that don't work well with the MustCall type hierarchy because it doesn't use the top type
 * as the default type.
 */
public class MustCallVisitor extends BaseTypeVisitor<MustCallAnnotatedTypeFactory> {

  /**
   * Creates a new MustCallVisitor.
   *
   * @param checker the type-checker associated with this visitor
   */
  public MustCallVisitor(BaseTypeChecker checker) {
    super(checker);
  }

  @Override
  public Void visitReturn(ReturnTree node, Void p) {
    // Only check return types if ownership is being transferred.
    MethodTree enclosingMethod = TreePathUtil.enclosingMethod(this.getCurrentPath());
    // enclosingMethod is null if this return site is inside a lambda. TODO: handle lambdas more
    // precisely?
    if (!checker.hasOption(MustCallChecker.NO_LIGHTWEIGHT_OWNERSHIP) && enclosingMethod != null) {
      ExecutableElement methodElt = TreeUtils.elementFromDeclaration(enclosingMethod);
      AnnotationMirror notOwningAnno = atypeFactory.getDeclAnnotation(methodElt, NotOwning.class);
      if (notOwningAnno != null) {
        // skip return type subtyping check, because not-owning pointer means OCC won't check anyway
        return null;
      }
    }
    return super.visitReturn(node, p);
  }

  @Override
  public boolean isValidUse(
      AnnotatedDeclaredType declarationType, AnnotatedDeclaredType useType, Tree tree) {
    // MustCallAlias annotations are always permitted on type uses, because these will be validated
    // by the
    // Object Construction Checker's -AcheckMustCall algorithm.
    if (!checker.hasOption(MustCallChecker.NO_RESOURCE_ALIASES)) {
      useType.removeAnnotationByClass(MustCallAlias.class);
    }
    return super.isValidUse(declarationType, useType, tree);
  }

  @Override
  protected boolean skipReceiverSubtypeCheck(
      MethodInvocationTree node,
      AnnotatedTypeMirror methodDefinitionReceiver,
      AnnotatedTypeMirror methodCallReceiver) {
    // Receivers cannot have must-call obligations - it doesn't make sense. If
    // the receiver of a method were to have a non-empty must-call obligation,
    // then actually the method **IS** a must-call method! So skipping this check is
    // always sound.
    return true;
  }

  /**
   * Mark (using the extraArgs parameter) any assignments where the LHS is a resource variable, so
   * that close doesn't need to be considered. See {@link
   * #commonAssignmentCheck(AnnotatedTypeMirror, AnnotatedTypeMirror, Tree, String, Object...)} for
   * the code that uses and removes the marks.
   */
  @Override
  protected void commonAssignmentCheck(
      Tree varTree,
      ExpressionTree valueExp,
      @CompilerMessageKey String errorKey,
      Object... extraArgs) {
    if (TreeUtils.elementFromTree(varTree).getKind() == ElementKind.RESOURCE_VARIABLE) {
      // Use the extraArgs array to signal to later stages of the common assignment check that this
      // is in a
      // resource variable context.
      Object[] newExtraArgs = Arrays.copyOf(extraArgs, extraArgs.length + 1);
      newExtraArgs[newExtraArgs.length - 1] = ElementKind.RESOURCE_VARIABLE;
      super.commonAssignmentCheck(varTree, valueExp, errorKey, newExtraArgs);
    } else {
      super.commonAssignmentCheck(varTree, valueExp, errorKey, extraArgs);
    }
  }

  /**
   * If the LHS has been marked as a resource variable, then the standard common assignment check is
   * skipped and a check that does not include "close" is substituted. Resource variables are marked
   * by {@link #commonAssignmentCheck(Tree, ExpressionTree, String, Object...)} using the extraArgs
   * parameter.
   */
  @Override
  protected void commonAssignmentCheck(
      AnnotatedTypeMirror varType,
      AnnotatedTypeMirror valueType,
      Tree valueTree,
      @CompilerMessageKey String errorKey,
      Object... extraArgs) {
    if (extraArgs.length >= 1 && extraArgs[extraArgs.length - 1] == ElementKind.RESOURCE_VARIABLE) {
      AnnotationMirror varAnno = varType.getAnnotationInHierarchy(atypeFactory.TOP);
      AnnotationMirror valAnno = valueType.getAnnotationInHierarchy(atypeFactory.TOP);
      if (atypeFactory
          .getQualifierHierarchy()
          .isSubtype(atypeFactory.withoutClose(valAnno), atypeFactory.withoutClose(varAnno))) {
        return;
      } else {
        // Remove the resource variable entry, since otherwise the rest of the framework could
        // interpret it as a format string (which is how extraArgs is usually used).
        extraArgs = Arrays.copyOf(extraArgs, extraArgs.length - 1);
      }
    }
    super.commonAssignmentCheck(varType, valueType, valueTree, errorKey, extraArgs);
  }

  /**
   * This method typically issues a warning if the result type of the constructor is not top,
   * because in top-default type systems that indicates a potential problem. The must call checker
   * does not need this warning, because it expects the type of all constructors to be {@code
   * MustCall({})} (by default) or some other {@code MustCall} type, not the top type.
   *
   * <p>Instead, this method checks that the result type of a constructor is a supertype of the
   * declared type on the class, if one exists.
   *
   * @param constructorType AnnotatedExecutableType for the constructor
   * @param constructorElement element that declares the constructor
   */
  @Override
  protected void checkConstructorResult(
      AnnotatedExecutableType constructorType, ExecutableElement constructorElement) {
    AnnotatedTypeMirror defaultType =
        atypeFactory.getAnnotatedType(ElementUtils.enclosingTypeElement(constructorElement));
    AnnotationMirror defaultAnno = defaultType.getAnnotationInHierarchy(atypeFactory.TOP);
    AnnotationMirror resultAnno =
        constructorType.getReturnType().getAnnotationInHierarchy(atypeFactory.TOP);
    if (!atypeFactory.getQualifierHierarchy().isSubtype(defaultAnno, resultAnno)) {
      checker.reportError(
          constructorElement, "inconsistent.constructor.type", resultAnno, defaultAnno);
    }
  }

  /**
   * Change the default for exception parameter lower bounds to bottom (the default), to prevent
   * false positives. This is unsound; see the discussion on
   * https://github.com/typetools/checker-framework/issues/3839. TODO: change checking of throws
   * clauses to require that the thrown exception is @MustCall({}). This would probably eliminate
   * most of the same false positives, without adding undue false positives.
   *
   * @return a set containing only the @MustCall({}) annotation
   */
  @Override
  protected Set<? extends AnnotationMirror> getExceptionParameterLowerBoundAnnotations() {
    return Collections.singleton(atypeFactory.BOTTOM);
  }

  /**
   * Does not issue any warnings.
   *
   * <p>This implementation prevents recursing into annotation arguments. Annotation arguments are
   * literals, which don't have must-call obligations.
   *
   * <p>Annotation arguments are treated as return locations for the purposes of defaulting, rather
   * than parameter locations. This causes them to default incorrectly when the annotation is
   * defined in bytecode. See https://github.com/typetools/checker-framework/issues/3178 for an
   * explanation of why this is necessary to avoid false positives.
   */
  @Override
  public Void visitAnnotation(AnnotationTree node, Void p) {
    return null;
  }
}
