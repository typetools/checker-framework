package org.checkerframework.framework.type.treeannotator;

import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.tree.UnaryTree;
import com.sun.source.util.TreePath;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeKind;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeFactory.ParameterizedExecutableType;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.Pair;
import org.checkerframework.javacutil.TreePathUtil;
import org.checkerframework.javacutil.TypeKindUtils;

/**
 * {@link PropagationTreeAnnotator} adds qualifiers to types where the resulting type is a function
 * of an input type, e.g. the result of a binary operation is a LUB of the type of expressions in
 * the binary operation.
 *
 * <p>{@link PropagationTreeAnnotator} is generally run first by {@link ListTreeAnnotator} since the
 * trees it handles are not usually targets of {@code @DefaultFor}.
 *
 * <p>{@link PropagationTreeAnnotator} does not traverse trees deeply by default.
 *
 * @see TreeAnnotator
 */
public class PropagationTreeAnnotator extends TreeAnnotator {

  private final QualifierHierarchy qualHierarchy;

  /** Creates a {@link PropagationTreeAnnotator} for the given {@code atypeFactory}. */
  public PropagationTreeAnnotator(AnnotatedTypeFactory atypeFactory) {
    super(atypeFactory);
    this.qualHierarchy = atypeFactory.getQualifierHierarchy();
  }

  @Override
  public Void visitNewArray(NewArrayTree tree, AnnotatedTypeMirror type) {
    assert type.getKind() == TypeKind.ARRAY
        : "PropagationTreeAnnotator.visitNewArray: should be an array type";

    AnnotatedTypeMirror componentType = ((AnnotatedArrayType) type).getComponentType();

    // prev is the lub of the initializers if they exist, otherwise the current component type.
    Set<? extends AnnotationMirror> prev = null;
    if (tree.getInitializers() != null && !tree.getInitializers().isEmpty()) {
      // We have initializers, either with or without an array type.

      // TODO (issue #599): This only works at the top level.  It should work at all levels of
      // the array.
      for (ExpressionTree init : tree.getInitializers()) {
        AnnotatedTypeMirror initType = atypeFactory.getAnnotatedType(init);
        // initType might be a typeVariable, so use effectiveAnnotations.
        Set<AnnotationMirror> annos = initType.getEffectiveAnnotations();

        prev = (prev == null) ? annos : qualHierarchy.leastUpperBounds(prev, annos);
      }
    } else {
      prev = componentType.getAnnotations();
    }

    assert prev != null
        : "PropagationTreeAnnotator.visitNewArray: violated assumption about qualifiers";

    TreePath path = atypeFactory.getPath(tree);
    AnnotatedTypeMirror contextType = null;
    if (atypeFactory.getVisitorState().getPath() != null
        && path != null
        && path.getParentPath() != null) {
      Tree parentTree = path.getParentPath().getLeaf();
      if (parentTree.getKind() == Kind.ASSIGNMENT) {
        Tree var = ((AssignmentTree) parentTree).getVariable();
        contextType = atypeFactory.getAnnotatedType(var);
      } else if (parentTree.getKind() == Kind.VARIABLE) {
        contextType = atypeFactory.getAnnotatedType(parentTree);
      } else if (parentTree instanceof CompoundAssignmentTree) {
        Tree var = ((CompoundAssignmentTree) parentTree).getVariable();
        contextType = atypeFactory.getAnnotatedType(var);
      } else if (parentTree.getKind() == Kind.RETURN) {
        Tree methodTree = TreePathUtil.enclosingMethodOrLambda(path.getParentPath());
        if (methodTree.getKind() == Kind.METHOD) {
          AnnotatedExecutableType methodType =
              atypeFactory.getAnnotatedType((MethodTree) methodTree);
          contextType = methodType.getReturnType();
        }
      } else if (parentTree.getKind() == Kind.METHOD_INVOCATION) {
        MethodInvocationTree methodInvocationTree = (MethodInvocationTree) parentTree;
        TreePath oldPath = atypeFactory.getVisitorState().getPath();
        atypeFactory.getVisitorState().setPath(null);
        ParameterizedExecutableType m = atypeFactory.methodFromUse(methodInvocationTree);
        for (int i = 0; i < m.executableType.getParameterTypes().size(); i++) {
          if (methodInvocationTree.getArguments().get(i) == tree) {
            contextType = m.executableType.getParameterTypes().get(i);
            break;
          }
        }
        atypeFactory.getVisitorState().setPath(oldPath);
      }
    }
    Set<? extends AnnotationMirror> post;

    if (contextType instanceof AnnotatedArrayType) {
      AnnotatedTypeMirror contextComponentType =
          ((AnnotatedArrayType) contextType).getComponentType();
      // Only compare the qualifiers that existed in the array type.
      // Defaulting wasn't performed yet, so prev might have fewer qualifiers than
      // contextComponentType, which would cause a failure.
      // TODO: better solution?
      boolean prevIsSubtype = true;
      for (AnnotationMirror am : prev) {
        if (contextComponentType.isAnnotatedInHierarchy(am)
            && !this.qualHierarchy.isSubtype(
                am, contextComponentType.getAnnotationInHierarchy(am))) {
          prevIsSubtype = false;
        }
      }
      // TODO: checking conformance of component kinds is a basic sanity check
      // It fails for array initializer expressions. Those should be handled nicer.
      if (contextComponentType.getKind() == componentType.getKind()
          && (prev.isEmpty()
              || (!contextComponentType.getAnnotations().isEmpty() && prevIsSubtype))) {
        post = contextComponentType.getAnnotations();
      } else {
        // The type of the array initializers is incompatible with the context type!
        // Somebody else will complain.
        post = prev;
      }
    } else {
      // No context is available - simply use what we have.
      post = prev;
    }
    // TODO (issue #599): This only works at the top level.  It should work at all levels of
    // the array.
    addAnnoOrBound(componentType, post);

    return null;
  }

  @Override
  public Void visitCompoundAssignment(CompoundAssignmentTree node, AnnotatedTypeMirror type) {
    if (hasPrimaryAnnotationInAllHierarchies(type)) {
      // If the type already has a primary annotation in all hierarchies, then the
      // propagated annotations won't be applied.  So don't compute them.
      return null;
    }
    AnnotatedTypeMirror rhs = atypeFactory.getAnnotatedType(node.getExpression());
    AnnotatedTypeMirror lhs = atypeFactory.getAnnotatedType(node.getVariable());
    Set<? extends AnnotationMirror> lubs =
        qualHierarchy.leastUpperBounds(
            rhs.getEffectiveAnnotations(), lhs.getEffectiveAnnotations());
    type.addMissingAnnotations(lubs);
    return null;
  }

  @Override
  public Void visitBinary(BinaryTree node, AnnotatedTypeMirror type) {
    if (hasPrimaryAnnotationInAllHierarchies(type)) {
      // If the type already has a primary annotation in all hierarchies, then the
      // propagated annotations won't be applied.  So don't compute them.
      // Also, calling getAnnotatedType on the left and right operands is potentially expensive.
      return null;
    }

    Pair<AnnotatedTypeMirror, AnnotatedTypeMirror> argTypes = atypeFactory.binaryTreeArgTypes(node);
    Set<? extends AnnotationMirror> lubs =
        qualHierarchy.leastUpperBounds(
            argTypes.first.getEffectiveAnnotations(), argTypes.second.getEffectiveAnnotations());
    type.addMissingAnnotations(lubs);

    return null;
  }

  @Override
  public Void visitUnary(UnaryTree node, AnnotatedTypeMirror type) {
    if (hasPrimaryAnnotationInAllHierarchies(type)) {
      // If the type already has a primary annotation in all hierarchies, then the
      // propagated annotations won't be applied.  So don't compute them.
      return null;
    }

    AnnotatedTypeMirror exp = atypeFactory.getAnnotatedType(node.getExpression());
    type.addMissingAnnotations(exp.getAnnotations());
    return null;
  }

  /*
   * TODO: would this make sense in general?
  @Override
  public Void visitConditionalExpression(ConditionalExpressionTree node, AnnotatedTypeMirror type) {
      if (!type.isAnnotated()) {
          AnnotatedTypeMirror a = typeFactory.getAnnotatedType(node.getTrueExpression());
          AnnotatedTypeMirror b = typeFactory.getAnnotatedType(node.getFalseExpression());
          Set<AnnotationMirror> lubs = qualHierarchy.leastUpperBounds(a.getEffectiveAnnotations(), b.getEffectiveAnnotations());
          type.replaceAnnotations(lubs);
      }
      return super.visitConditionalExpression(node, type);
  }*/

  @Override
  public Void visitTypeCast(TypeCastTree node, AnnotatedTypeMirror type) {
    if (hasPrimaryAnnotationInAllHierarchies(type)) {
      // If the type is already has a primary annotation in all hierarchies, then the
      // propagated annotations won't be applied.  So don't compute them.
      return null;
    }

    AnnotatedTypeMirror exprType = atypeFactory.getAnnotatedType(node.getExpression());
    if (type.getKind() == TypeKind.TYPEVAR) {
      if (exprType.getKind() == TypeKind.TYPEVAR) {
        // If both types are type variables, take the direct annotations.
        type.addMissingAnnotations(exprType.getAnnotations());
      }
      // else do nothing.
    } else {
      // Use effective annotations from the expression, to get upper bound of type variables.
      Set<AnnotationMirror> expressionAnnos = exprType.getEffectiveAnnotations();

      TypeKind castKind = type.getPrimitiveKind();
      if (castKind != null) {
        TypeKind exprKind = exprType.getPrimitiveKind();
        if (exprKind != null) {
          switch (TypeKindUtils.getPrimitiveConversionKind(exprKind, castKind)) {
            case WIDENING:
              expressionAnnos =
                  atypeFactory.getWidenedAnnotations(expressionAnnos, exprKind, castKind);
              break;
            case NARROWING:
              atypeFactory.getNarrowedAnnotations(expressionAnnos, exprKind, castKind);
              break;
            case SAME:
              // Nothing to do
              break;
          }
        }
      }

      // If the qualifier on the expression type is a supertype of the qualifier upper bound
      // of the cast type, then apply the bound as the default qualifier rather than the
      // expression qualifier.
      addAnnoOrBound(type, expressionAnnos);
    }

    return null;
  }

  private boolean hasPrimaryAnnotationInAllHierarchies(AnnotatedTypeMirror type) {
    boolean annotated = true;
    for (AnnotationMirror top : qualHierarchy.getTopAnnotations()) {
      if (type.getEffectiveAnnotationInHierarchy(top) == null) {
        annotated = false;
      }
    }
    return annotated;
  }

  /**
   * Adds the qualifiers in {@code annos} to {@code type} that are below the qualifier upper bound
   * of type and for which type does not already have annotation in the same hierarchy. If a
   * qualifier in {@code annos} is above the bound, then the bound is added to {@code type} instead.
   *
   * @param type annotations are added to this type
   * @param annos annotations to add to type
   */
  private void addAnnoOrBound(AnnotatedTypeMirror type, Set<? extends AnnotationMirror> annos) {
    Set<AnnotationMirror> boundAnnos =
        atypeFactory.getQualifierUpperBounds().getBoundQualifiers(type.getUnderlyingType());
    Set<AnnotationMirror> annosToAdd = AnnotationUtils.createAnnotationSet();
    for (AnnotationMirror boundAnno : boundAnnos) {
      AnnotationMirror anno = qualHierarchy.findAnnotationInSameHierarchy(annos, boundAnno);
      if (anno != null && !qualHierarchy.isSubtype(anno, boundAnno)) {
        annosToAdd.add(boundAnno);
      }
    }
    type.addMissingAnnotations(annosToAdd);
    type.addMissingAnnotations(annos);
  }
}
