package org.checkerframework.checker.nullness;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.VariableTree;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeKind;
import org.checkerframework.checker.nullness.KeyForPropagator.PropagationDirection;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.javacutil.TreeUtils;

/**
 * For the following initializations we wish to propagate the annotations from the left-hand side to
 * the right-hand side or vice versa:
 *
 * <p>1. If a keySet is being saved to a newly declared set, we transfer the annotations from the
 * keySet to the lhs. e.g.,
 *
 * <pre>{@code
 * // The user is not required to explicitly annotate the LHS's type argument with @KeyFor("m")
 * Set<String> keySet = m.keySet();
 * }</pre>
 *
 * 2. If a variable declaration contains type arguments with an @KeyFor annotation and its
 * initializer is a new class tree with corresponding type arguments that have an @UknownKeyFor
 * primary annotation, we transfer from the LHS to RHS. e.g.,
 *
 * <pre>{@code
 * // The user does not have to write @KeyFor("m") on both sides
 * List<@KeyFor("m") String> keys = new ArrayList<String>();
 * }</pre>
 *
 * 3. IMPORTANT NOTE: The following case must be (and is) handled in KeyForAnnotatedTypeFactory. In
 * BaseTypeVisitor we check to make sure that the constructor called in a NewClassTree is actually
 * compatible with the annotations placed on the NewClassTree. This requires that, prior to this
 * check we also propagate the annotations to this constructor in constructorFromUse so that the
 * constructor call matches the type given to the NewClassTree.
 *
 * @see
 *     org.checkerframework.checker.nullness.KeyForAnnotatedTypeFactory#constructorFromUse(com.sun.source.tree.NewClassTree)
 *     <p>Note propagation only occurs between two AnnotatedDeclaredTypes. If either side is not an
 *     AnnotatedDeclaredType then this class does nothing.
 */
public class KeyForPropagationTreeAnnotator extends TreeAnnotator {
  private final KeyForPropagator keyForPropagator;
  private final ExecutableElement keySetMethod;

  public KeyForPropagationTreeAnnotator(
      AnnotatedTypeFactory atypeFactory, KeyForPropagator propagationTreeAnnotator) {
    super(atypeFactory);
    this.keyForPropagator = propagationTreeAnnotator;
    keySetMethod =
        TreeUtils.getMethod("java.util.Map", "keySet", 0, atypeFactory.getProcessingEnv());
  }

  /**
   * Returns true iff expression is a call to java.util.Map.KeySet.
   *
   * @return true iff expression is a call to java.util.Map.KeySet
   */
  public boolean isCallToKeyset(ExpressionTree expression) {
    return TreeUtils.isMethodInvocation(expression, keySetMethod, atypeFactory.getProcessingEnv());
  }

  /**
   * Transfers annotations on type arguments from the initializer to the variableTree, if the
   * initializer is a call to java.util.Map.keySet.
   */
  @Override
  public Void visitVariable(VariableTree variableTree, AnnotatedTypeMirror type) {
    super.visitVariable(variableTree, type);

    // This should only happen on Map.keySet();
    if (type.getKind() == TypeKind.DECLARED) {
      final ExpressionTree initializer = variableTree.getInitializer();

      if (isCallToKeyset(initializer)) {
        final AnnotatedDeclaredType variableType = (AnnotatedDeclaredType) type;
        final AnnotatedTypeMirror initializerType = atypeFactory.getAnnotatedType(initializer);

        // Propagate just for declared (class) types, not for array types, boxed primitives, etc.
        if (variableType.getKind() == TypeKind.DECLARED) {
          keyForPropagator.propagate(
              (AnnotatedDeclaredType) initializerType,
              variableType,
              PropagationDirection.TO_SUPERTYPE,
              atypeFactory);
        }
      }
    }

    return null;
  }

  /** Transfers annotations to type if the left hand side is a variable declaration. */
  @Override
  public Void visitNewClass(NewClassTree node, AnnotatedTypeMirror type) {
    keyForPropagator.propagateNewClassTree(node, type, (KeyForAnnotatedTypeFactory) atypeFactory);
    return super.visitNewClass(node, type);
  }
}
