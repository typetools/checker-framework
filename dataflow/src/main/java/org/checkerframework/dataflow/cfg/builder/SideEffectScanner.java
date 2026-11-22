package org.checkerframework.dataflow.cfg.builder;

import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.EnhancedForLoopTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TryTree;
import com.sun.source.tree.UnaryTree;
import com.sun.source.util.TreeScanner;
import javax.lang.model.element.RecordComponentElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.javacutil.AnnotationProvider;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypesUtils;

/**
 * Determines whether evaluating a tree might have a side effect. A call to a method or constructor
 * has a side effect unless the annotation provider considers the method or constructor
 * side-effect-free, which is the same test that a store uses when deciding whether a call undoes
 * flow-sensitive refinement. This includes a call that the source code does not write: matching a
 * deconstruction pattern calls all of the record's accessor methods. This class treats other code
 * as always having a side effect:
 *
 * <ul>
 *   <li>an assignment, a compound assignment, or an increment or decrement;
 *   <li>instantiation of an anonymous class, whose initializers run;
 *   <li>string concatenation with an operand whose type is a reference type other than {@code
 *       String} or a boxed primitive type, which calls the operand's {@code toString()} method;
 *   <li>an enhanced {@code for} loop, which calls {@code iterator()}, {@code hasNext()}, and {@code
 *       next()} if it iterates over an {@code Iterable}; and
 *   <li>a try-with-resources statement, which calls {@code close()}.
 * </ul>
 *
 * <p>Evaluating a lambda expression or a class declaration does not run its body, so the body has
 * no side effect.
 *
 * <p>The entry point is {@link #mightHaveSideEffects(Tree, AnnotationProvider)}.
 */
/*package-private*/ class SideEffectScanner extends TreeScanner<Void, Void> {

  /** The annotation provider, which determines whether a method is side-effect-free. */
  private final AnnotationProvider annotationProvider;

  /** True if a scanned tree might have a side effect. */
  private boolean mightHaveSideEffects = false;

  /**
   * Creates a SideEffectScanner.
   *
   * @param annotationProvider the annotation provider, which determines whether a method is
   *     side-effect-free
   */
  private SideEffectScanner(AnnotationProvider annotationProvider) {
    this.annotationProvider = annotationProvider;
  }

  /**
   * Returns true if evaluating {@code tree} might have a side effect, such as an assignment or a
   * call to a method that is not side-effect-free. Evaluating a lambda expression or a class
   * declaration does not run its body, so this method does not examine the body.
   *
   * @param tree an expression or a pattern
   * @param annotationProvider the annotation provider, which determines whether a method is
   *     side-effect-free
   * @return true if evaluating {@code tree} might have a side effect
   */
  /*package-private*/ static boolean mightHaveSideEffects(
      Tree tree, AnnotationProvider annotationProvider) {
    SideEffectScanner scanner = new SideEffectScanner(annotationProvider);
    scanner.scan(tree, null);
    return scanner.mightHaveSideEffects;
  }

  @Override
  public Void scan(Tree tree, Void p) {
    if (tree != null && TreeUtils.isDeconstructionPatternTree(tree)) {
      // Matching a deconstruction pattern calls the record's accessor methods.
      TypeElement recordElt = (TypeElement) ((DeclaredType) TreeUtils.typeOf(tree)).asElement();
      for (RecordComponentElement component : recordElt.getRecordComponents()) {
        if (!annotationProvider.isSideEffectFree(component.getAccessor())) {
          mightHaveSideEffects = true;
        }
      }
    }
    return super.scan(tree, p);
  }

  @Override
  public Void visitMethodInvocation(MethodInvocationTree tree, Void p) {
    if (!annotationProvider.isSideEffectFree(TreeUtils.elementFromUse(tree))) {
      mightHaveSideEffects = true;
    }
    return super.visitMethodInvocation(tree, p);
  }

  @Override
  public Void visitNewClass(NewClassTree tree, Void p) {
    if (tree.getClassBody() != null) {
      // It's an anonymous class.  Its initializers run when it is instantiated.
      mightHaveSideEffects = true;
    } else if (!annotationProvider.isSideEffectFree(TreeUtils.elementFromUse(tree))) {
      mightHaveSideEffects = true;
    }
    return super.visitNewClass(tree, p);
  }

  @Override
  public Void visitAssignment(AssignmentTree tree, Void p) {
    mightHaveSideEffects = true;
    return super.visitAssignment(tree, p);
  }

  @Override
  public Void visitCompoundAssignment(CompoundAssignmentTree tree, Void p) {
    mightHaveSideEffects = true;
    return super.visitCompoundAssignment(tree, p);
  }

  @Override
  public Void visitBinary(BinaryTree tree, Void p) {
    // String concatenation calls toString() on each operand that is a reference (JLS 5.1.11).
    if (tree.getKind() == Tree.Kind.PLUS && TypesUtils.isString(TreeUtils.typeOf(tree))) {
      if (stringConversionMightHaveSideEffects(TreeUtils.typeOf(tree.getLeftOperand()))
          || stringConversionMightHaveSideEffects(TreeUtils.typeOf(tree.getRightOperand()))) {
        mightHaveSideEffects = true;
      }
    }
    return super.visitBinary(tree, p);
  }

  /**
   * Returns true if string conversion (JLS 5.1.11) of a value of the given type might have a side
   * effect. String conversion of a primitive, a boxed primitive, a {@code String}, or {@code null}
   * has no side effect. String conversion of any other reference calls its {@code toString()}
   * method, which might have a side effect.
   *
   * @param type the type of an operand of string concatenation
   * @return true if string conversion of a value of the given type might have a side effect
   */
  private static boolean stringConversionMightHaveSideEffects(TypeMirror type) {
    return !(type.getKind().isPrimitive()
        || type.getKind() == TypeKind.NULL
        || TypesUtils.isString(type)
        || TypesUtils.isBoxedPrimitive(type));
  }

  @Override
  public Void visitUnary(UnaryTree tree, Void p) {
    switch (tree.getKind()) {
      case POSTFIX_DECREMENT, POSTFIX_INCREMENT, PREFIX_DECREMENT, PREFIX_INCREMENT ->
          mightHaveSideEffects = true;
      default -> {}
    }
    return super.visitUnary(tree, p);
  }

  @Override
  public Void visitEnhancedForLoop(EnhancedForLoopTree tree, Void p) {
    // A loop over an Iterable calls iterator(), hasNext(), and next().
    mightHaveSideEffects = true;
    return super.visitEnhancedForLoop(tree, p);
  }

  @Override
  public Void visitTry(TryTree tree, Void p) {
    if (!tree.getResources().isEmpty()) {
      // A try-with-resources statement calls close().
      mightHaveSideEffects = true;
    }
    return super.visitTry(tree, p);
  }

  @Override
  public Void visitLambdaExpression(LambdaExpressionTree tree, Void p) {
    // Evaluating a lambda expression does not run its body.
    return null;
  }

  @Override
  public Void visitClass(ClassTree tree, Void p) {
    // A class declaration runs no code.
    return null;
  }
}
