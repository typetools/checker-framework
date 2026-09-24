package org.checkerframework.checker.modifiability;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MethodTree;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeKind;
import org.checkerframework.checker.modifiability.qual.PreservesModifiability;
import org.checkerframework.checker.modifiability.qual.ThrowsUnsupportedOperation;
import org.checkerframework.checker.modifiability.qual.UnmodifiableParam;
import org.checkerframework.framework.source.SourceVisitor;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.TreeUtils;

/** Visitor for the aggregate ModifiabilityChecker. */
public class ModifiabilityVisitor extends SourceVisitor<Void, Void> {

  /** Fully-qualified name for {@link UnmodifiableParam}. */
  private static final String unmodifiableParamQualifiedName = UnmodifiableParam.class.getName();

  /** Fully-qualified name for {@link PreservesModifiability}. */
  private static final String preservesModifiabilityQualifiedName =
      PreservesModifiability.class.getName();

  /** {@link ModifiabilityChecker}. */
  private final ModifiabilityChecker checker;

  /** True when scanning a formal parameter type, including the receiver parameter. */
  private boolean inParameterType = false;

  /**
   * Creates a {@link SourceVisitor} to use for scanning a source tree.
   *
   * @param checker the modifiability checker to invoke on the input source tree
   */
  protected ModifiabilityVisitor(ModifiabilityChecker checker) {
    super(checker);
    this.checker = checker;
  }

  /**
   * Sets {@link #inParameterType} while scanning this method's formal and receiver parameters, so
   * that {@link #visitAnnotation} can distinguish an allowed {@code @UnmodifiableParam} in a
   * parameter type from a disallowed one elsewhere in the same method.
   *
   * <p>The field needs no save-and-restore discipline, because neither a method declaration nor a
   * lambda expression can appear within a formal parameter. It is reset in a {@code finally} clause
   * because this visitor is reused for every compilation unit, and a stale true value would
   * suppress every subsequent {@code unmodparam.location} error.
   */
  @Override
  public Void visitMethod(MethodTree tree, Void p) {
    storeSuppressWarningsAnno(tree);
    checkPreservesModifiabilityLocation(tree);
    checkThrowsUnsupportedOperation(tree);
    scan(tree.getModifiers(), p);
    scan(tree.getReturnType(), p);
    scan(tree.getTypeParameters(), p);
    inParameterType = true;
    try {
      scan(tree.getParameters(), p);
      scan(tree.getReceiverParameter(), p);
    } finally {
      inParameterType = false;
    }
    scan(tree.getThrows(), p);
    scan(tree.getBody(), p);
    scan(tree.getDefaultValue(), p);
    return null;
  }

  /**
   * Sets {@link #inParameterType} while scanning this lambda's formal parameters, so that an
   * explicitly-typed lambda parameter may be annotated {@code @UnmodifiableParam}, just as a method
   * formal parameter may. An implicitly-typed lambda parameter has no type to annotate.
   */
  @Override
  public Void visitLambdaExpression(LambdaExpressionTree tree, Void p) {
    inParameterType = true;
    try {
      scan(tree.getParameters(), p);
    } finally {
      inParameterType = false;
    }
    scan(tree.getBody(), p);
    return null;
  }

  /**
   * Issues an error if {@code tree} is annotated {@code @ThrowsUnsupportedOperation} but its body
   * is not exactly {@code throw new UnsupportedOperationException(...)}.
   *
   * <p>The annotation is a promise that other classes rely on: a class that inherits the method
   * without overriding it does not support the operation. A method with no body, such as an
   * abstract method, has no implementation to make the promise about, so it is an error too.
   *
   * <p>This check is in the aggregate checker rather than in {@link ModifiabilityBaseVisitor}
   * because it does not depend on a modifiability hierarchy; running it in each sub-checker would
   * issue the same error five times.
   *
   * @param tree a method declaration
   */
  private void checkThrowsUnsupportedOperation(MethodTree tree) {
    ExecutableElement methodElt = TreeUtils.elementFromDeclaration(tree);
    if (methodElt == null || methodElt.getAnnotation(ThrowsUnsupportedOperation.class) == null) {
      return;
    }
    if (!ModifiabilityBaseVisitor.implIsUOE(tree, types, elements)) {
      checker.reportError(tree, "throwsuoe.implementation.not.uoe");
    }
  }

  /**
   * Issues an error if {@code tree} is annotated as {@code @PreservesModifiability} but is not a
   * method that has exactly one formal parameter, which is not a varargs parameter, and a non-void
   * result.
   *
   * <p>The annotation relates the method's result to its first argument, so it says nothing about
   * such a method. Worse, on a method with more than one formal parameter it would silently use the
   * first argument, which need not be the one that the programmer had in mind. A varargs method is
   * rejected for the same reason: a call's first argument is an element of the varargs array rather
   * than the sole formal parameter, so the annotation would relate the result to a value of a
   * different type.
   *
   * @param tree a method declaration
   */
  private void checkPreservesModifiabilityLocation(MethodTree tree) {
    ExecutableElement methodElt = TreeUtils.elementFromDeclaration(tree);
    if (methodElt == null || methodElt.getAnnotation(PreservesModifiability.class) == null) {
      return;
    }
    if (methodElt.getParameters().size() == 1
        && !methodElt.isVarArgs()
        && methodElt.getReturnType().getKind() != TypeKind.VOID) {
      return;
    }
    for (AnnotationTree annoTree : tree.getModifiers().getAnnotations()) {
      AnnotationMirror anno = TreeUtils.annotationFromAnnotationTree(annoTree);
      if (anno != null
          && AnnotationUtils.areSameByName(anno, preservesModifiabilityQualifiedName)) {
        checker.reportError(annoTree, "preservesmodifiability.location");
        return;
      }
    }
    checker.reportError(tree, "preservesmodifiability.location");
  }

  @Override
  public Void visitAnnotation(AnnotationTree tree, Void p) {
    // The implementation of annotationFromAnnotationTree just returns a field of tree, so it's fine
    // to always get it.
    AnnotationMirror annotation = TreeUtils.annotationFromAnnotationTree(tree);
    if (!inParameterType
        && annotation != null
        && AnnotationUtils.areSameByName(annotation, unmodifiableParamQualifiedName)) {
      checker.reportError(tree, "unmodparam.location");
    }
    return super.visitAnnotation(tree, p);
  }
}
