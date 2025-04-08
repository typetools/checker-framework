package org.checkerframework.checker.confidential;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.CompoundAssignmentTree;
import java.util.Set;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import org.checkerframework.checker.confidential.qual.BottomConfidential;
import org.checkerframework.checker.confidential.qual.Confidential;
import org.checkerframework.checker.confidential.qual.NonConfidential;
import org.checkerframework.checker.confidential.qual.UnknownConfidential;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationMirrorSet;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TreeUtils;

/** Annotated type factory for the Confidential Checker. */
public class ConfidentialAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

  /** The {@code @}{@link NonConfidential} annotation mirror. */
  private final AnnotationMirror NONCONFIDENTIAL;

  /** The {@code @}{@link Confidential} annotation mirror. */
  private final AnnotationMirror CONFIDENTIAL;

  /** The {@code @}{@link UnknownConfidential} annotation mirror. */
  private final AnnotationMirror UNKNOWN_CONFIDENTIAL;

  /** The {@code @}{@link BottomConfidential} annotation mirror. */
  private final AnnotationMirror BOTTOM_CONFIDENTIAL;

  /** A singleton set containing the {@code @}{@link NonConfidential} annotation mirror. */
  private final AnnotationMirrorSet setOfNonConfidential;

  /**
   * Creates a {@link ConfidentialAnnotatedTypeFactory}.
   *
   * @param checker the confidential checker
   */
  public ConfidentialAnnotatedTypeFactory(BaseTypeChecker checker) {
    super(checker);
    this.NONCONFIDENTIAL = AnnotationBuilder.fromClass(getElementUtils(), NonConfidential.class);
    this.CONFIDENTIAL = AnnotationBuilder.fromClass(getElementUtils(), Confidential.class);
    this.UNKNOWN_CONFIDENTIAL =
        AnnotationBuilder.fromClass(getElementUtils(), UnknownConfidential.class);
    this.BOTTOM_CONFIDENTIAL =
        AnnotationBuilder.fromClass(getElementUtils(), BottomConfidential.class);
    this.setOfNonConfidential = AnnotationMirrorSet.singleton(NONCONFIDENTIAL);
    postInit();
  }

  @Override
  protected Set<AnnotationMirror> getEnumConstructorQualifiers() {
    return setOfNonConfidential;
  }

  @Override
  public TreeAnnotator createTreeAnnotator() {
    return new ListTreeAnnotator(
        super.createTreeAnnotator(),
        new ConfidentialAnnotatedTypeFactory.ConfidentialTreeAnnotator(this));
  }

  /**
   * A TreeAnnotator to enforce Confidential String concatenation rules:
   *
   * <ul>
   *   <li>(Confidential + NonConfidential) returns Confidential (commutatively);
   *   <li>(Confidential + Confidential) returns Confidential;
   *   <li>(NonConfidential + NonConfidential) returns NonConfidential;
   *   <li>UnknownConfidential dominates other types in concatenation;
   *   <li>Non-bottom types dominate BottomConfidential in concatenation.
   * </ul>
   */
  private class ConfidentialTreeAnnotator extends TreeAnnotator {
    /**
     * Creates a {@link ConfidentialAnnotatedTypeFactory.ConfidentialTreeAnnotator}
     *
     * @param atypeFactory the annotated type factory
     */
    public ConfidentialTreeAnnotator(AnnotatedTypeFactory atypeFactory) {
      super(atypeFactory);
    }

    @Override
    public Void visitBinary(BinaryTree tree, AnnotatedTypeMirror type) {
      if (TreeUtils.isStringConcatenation(tree)) {
        AnnotatedTypeMirror leftType = getAnnotatedType(tree.getLeftOperand());
        AnnotatedTypeMirror rightType = getAnnotatedType(tree.getRightOperand());
        type.replaceAnnotation(getResultingType(leftType, rightType));
      }
      return null;
    }

    @Override
    public Void visitCompoundAssignment(CompoundAssignmentTree tree, AnnotatedTypeMirror type) {
      if (TreeUtils.isStringCompoundConcatenation(tree)) {
        AnnotatedTypeMirror leftType = getAnnotatedType(tree.getVariable());
        AnnotatedTypeMirror rightType = getAnnotatedType(tree.getExpression());
        type.replaceAnnotation(getResultingType(leftType, rightType));
      }
      return null;
    }

    /**
     * Returns the type of concatenating leftType and rightType.
     *
     * @param leftType the type on the left of the expression
     * @param rightType the type on the right of the expression
     * @return the resulting type after concatenation
     */
    private AnnotationMirror getResultingType(
        AnnotatedTypeMirror leftType, AnnotatedTypeMirror rightType) {
      if (leftType.hasPrimaryAnnotation(UNKNOWN_CONFIDENTIAL)
          || rightType.hasPrimaryAnnotation(UNKNOWN_CONFIDENTIAL)) {
        return UNKNOWN_CONFIDENTIAL;
      }

      if (leftType.hasPrimaryAnnotation(BOTTOM_CONFIDENTIAL)) {
        return rightType.getPrimaryAnnotation();
      } else if (rightType.hasPrimaryAnnotation(BOTTOM_CONFIDENTIAL)) {
        return leftType.getPrimaryAnnotation();
      }

      if (leftType.hasPrimaryAnnotation(CONFIDENTIAL)
          || rightType.hasPrimaryAnnotation(CONFIDENTIAL)) {
        return CONFIDENTIAL;
      }

      return NONCONFIDENTIAL;
    }
  }

  /**
   * Defines specific type-checking rules for Object.toString() that allow
   * @NonConfidential Objects to return @NonConfidential Strings.
   *
   * @param tree an AST node
   * @param type the type obtained from tree
   * @param useFlow whether to use information from dataflow analysis
   */
  @Override
  public void addComputedTypeAnnotations(Tree tree, AnnotatedTypeMirror type, boolean useFlow) {
    if (TreeUtils.isMethodAccess(tree)) {
      MethodInvocationTree methodInvoc = (MethodInvocationTree) tree;
      if (TreeUtils.isMethodInvocation(methodInvoc,
          TreeUtils.getMethod(Object.class, "toString", 1, processingEnv),
          processingEnv)) {
        Element arg = TreeUtils.elementFromTree(methodInvoc.getTypeArguments().get(0));
        if (fromElement(arg).hasPrimaryAnnotation(NONCONFIDENTIAL)) {
          type.replaceAnnotation(NONCONFIDENTIAL);
        }
      }
    }
    super.addComputedTypeAnnotations(tree, type, useFlow);
  }
}
