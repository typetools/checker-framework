package org.checkerframework.framework.type.treeannotator;

import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.tree.UnaryTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import java.util.Map;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.javacutil.AnnotationMirrorSet;
import org.checkerframework.javacutil.TreePathUtil;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypeKindUtils;
import org.plumelib.util.IPair;
import org.plumelib.util.MapsP;

/**
 * {@link PropagationTreeAnnotator} adds qualifiers to types where the resulting type is a function
 * of an input type.
 *
 * <p>By default, it applies LUBs, e.g., the result of a binary operation is a LUB of the type of
 * expressions in the binary operation, and likewise for compound operations. It also handles unary
 * expressions, array creation, and casts.
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

  /**
   * If true, use the assignment context when computing the type of a new array expression. This is
   * a hack to prevent infinite recursion if computing the type of the assignment context includes
   * computing the type of the right-hand side of the assignment. This happens when the assignment
   * is the pseudo-assignment of a method argument to a formal parameter.
   */
  private boolean useAssignmentContext = true;

  /**
   * A mapping from {@code MethodInvocationTree} to the type of its declaration adapted to the call
   * site. This is a cache used when getting the type of a new array expression that is an argument
   * to a method. Getting the call-site-adapted type of a method also gets the type of all the
   * arguments at the call site. (This happens both for resolving polymorphic methods and for method
   * type argument inference.) {@link #useAssignmentContext} is used to prevent infinite recursion
   * and this cache is used to improve performance.
   */
  private final Map<MethodInvocationTree, AnnotatedExecutableType> methodInvocationToType =
      MapsP.createLruCache(300);

  @Override
  public Void visitNewArray(NewArrayTree arrayTree, AnnotatedTypeMirror arrayType) {
    assert arrayType.getKind() == TypeKind.ARRAY
        : "PropagationTreeAnnotator.visitNewArray: should be an array type";

    AnnotatedTypeMirror componentType = ((AnnotatedArrayType) arrayType).getComponentType();
    TypeMirror componentTM = componentType.getUnderlyingType();

    // prev is the lub of the initializers if they exist, otherwise the current component type.
    Set<? extends AnnotationMirror> prev = null;
    if (arrayTree.getInitializers() != null && !arrayTree.getInitializers().isEmpty()) {
      // We have initializers, either with or without an array type.

      // TODO (issue #599): This only works at the top level.  It should work at all levels of
      // the array.
      for (ExpressionTree init : arrayTree.getInitializers()) {
        AnnotatedTypeMirror initType = atypeFactory.getAnnotatedType(init);
        // initType might be a typeVariable, so use effectiveAnnotations.
        AnnotationMirrorSet annos = initType.getEffectiveAnnotations();

        prev =
            (prev == null)
                ? annos
                : qualHierarchy.leastUpperBoundsShallow(
                    prev, componentTM, annos, initType.getUnderlyingType());
      }
    } else {
      prev = componentType.getPrimaryAnnotations();
    }

    assert prev != null
        : "PropagationTreeAnnotator.visitNewArray: violated assumption about qualifiers";

    TreePath path = atypeFactory.getPath(arrayTree);
    AnnotatedTypeMirror contextType = null;
    if (path != null && path.getParentPath() != null) {
      Tree parentTree = path.getParentPath().getLeaf();
      if (parentTree instanceof AssignmentTree) {
        Tree var = ((AssignmentTree) parentTree).getVariable();
        contextType = atypeFactory.getAnnotatedType(var);
      } else if (parentTree instanceof VariableTree) {
        if (!TreeUtils.isVariableTreeDeclaredUsingVar((VariableTree) parentTree)) {
          contextType = atypeFactory.getAnnotatedType(parentTree);
        }
      } else if (parentTree instanceof CompoundAssignmentTree) {
        Tree var = ((CompoundAssignmentTree) parentTree).getVariable();
        contextType = atypeFactory.getAnnotatedType(var);
      } else if (parentTree instanceof ReturnTree) {
        Tree methodTree = TreePathUtil.enclosingMethodOrLambda(path.getParentPath());
        if (methodTree instanceof MethodTree) {
          AnnotatedExecutableType methodType =
              atypeFactory.getAnnotatedType((MethodTree) methodTree);
          contextType = methodType.getReturnType();
        }
      } else if (parentTree instanceof MethodInvocationTree && useAssignmentContext) {
        MethodInvocationTree methodInvocationTree = (MethodInvocationTree) parentTree;
        useAssignmentContext = false;
        AnnotatedExecutableType m;
        try {
          if (atypeFactory.shouldCache
              && methodInvocationToType.containsKey(methodInvocationTree)) {
            m = methodInvocationToType.get(methodInvocationTree);
          } else {
            m = atypeFactory.methodFromUse(methodInvocationTree).executableType;
            if (atypeFactory.shouldCache) {
              methodInvocationToType.put(methodInvocationTree, m);
            }
          }
        } finally {
          useAssignmentContext = true;
        }
        for (int i = 0; i < m.getParameterTypes().size(); i++) {
          @SuppressWarnings("interning") // Tree must be exactly the same.
          boolean foundArgument = methodInvocationTree.getArguments().get(i) == arrayTree;
          if (foundArgument) {
            contextType = m.getParameterTypes().get(i);
            break;
          }
        }
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
      TypeMirror contextCTM = contextComponentType.getUnderlyingType();
      boolean prevIsSubtype = true;
      for (AnnotationMirror am : prev) {
        if (contextComponentType.hasPrimaryAnnotationInHierarchy(am)
            && !this.qualHierarchy.isSubtypeShallow(
                am,
                contextCTM,
                contextComponentType.getPrimaryAnnotationInHierarchy(am),
                contextCTM)) {
          prevIsSubtype = false;
        }
      }
      // TODO: checking conformance of component kinds is a basic sanity check
      // It fails for array initializer expressions. Those should be handled nicer.
      if (contextComponentType.getKind() == componentType.getKind()
          && (prev.isEmpty()
              || (!contextComponentType.getPrimaryAnnotations().isEmpty() && prevIsSubtype))) {
        post = contextComponentType.getPrimaryAnnotations();
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
  public Void visitCompoundAssignment(CompoundAssignmentTree tree, AnnotatedTypeMirror type) {
    if (hasPrimaryAnnotationInAllHierarchies(type)) {
      // If the type already has a primary annotation in all hierarchies, then the
      // propagated annotations won't be applied.  So don't compute them.
      return null;
    }
    AnnotatedTypeMirror rhs = atypeFactory.getAnnotatedType(tree.getExpression());
    AnnotatedTypeMirror lhs = atypeFactory.getAnnotatedType(tree.getVariable());
    Set<? extends AnnotationMirror> lubs =
        qualHierarchy.leastUpperBoundsShallow(
            rhs.getEffectiveAnnotations(),
            rhs.getUnderlyingType(),
            lhs.getEffectiveAnnotations(),
            lhs.getUnderlyingType());
    type.addMissingAnnotations(lubs);
    return null;
  }

  @Override
  public Void visitBinary(BinaryTree tree, AnnotatedTypeMirror type) {
    if (hasPrimaryAnnotationInAllHierarchies(type)) {
      // If the type already has a primary annotation in all hierarchies, then the
      // propagated annotations won't be applied.  So don't compute them.
      // Also, calling getAnnotatedType on the left and right operands is potentially
      // expensive.
      return null;
    }

    if (!((GenericAnnotatedTypeFactory) atypeFactory).isRelevant(type)) {
      return null;
    }

    IPair<AnnotatedTypeMirror, AnnotatedTypeMirror> argTypes =
        atypeFactory.binaryTreeArgTypes(tree);
    AnnotatedTypeMirror type1 = argTypes.first;
    AnnotatedTypeMirror type2 = argTypes.second;
    Set<? extends AnnotationMirror> lubs =
        qualHierarchy.leastUpperBoundsShallow(
            type1.getEffectiveAnnotations(),
            type1.getUnderlyingType(),
            type2.getEffectiveAnnotations(),
            type2.getUnderlyingType());
    log(
        "%s PTA.visitBinary(%s, %s)%n  argTypes=%s%n  lubs=%s%n",
        atypeFactory.getClass().getSimpleName(), tree, type, argTypes, lubs);
    type.addMissingAnnotations(lubs);
    log("PTA.visitBinary(%s, ...): final type = %s%n", tree, type);

    return null;
  }

  @Override
  public Void visitUnary(UnaryTree tree, AnnotatedTypeMirror type) {
    if (hasPrimaryAnnotationInAllHierarchies(type)) {
      // If the type already has a primary annotation in all hierarchies, then the
      // propagated annotations won't be applied.  So don't compute them.
      return null;
    }

    AnnotatedTypeMirror exp = atypeFactory.getAnnotatedType(tree.getExpression());
    type.addMissingAnnotations(exp.getPrimaryAnnotations());
    return null;
  }

  /*
   * TODO: would this make sense in general?
  @Override
  public Void visitConditionalExpression(ConditionalExpressionTree tree, AnnotatedTypeMirror type) {
      if (!type.isAnnotated()) {
          AnnotatedTypeMirror a = typeFactory.getAnnotatedType(tree.getTrueExpression());
          AnnotatedTypeMirror b = typeFactory.getAnnotatedType(tree.getFalseExpression());
          AnnotationMirrorSet lubs = qualHierarchy.leastUpperBounds(a.getEffectiveAnnotations(), b.getEffectiveAnnotations());
          type.replaceAnnotations(lubs);
      }
      return super.visitConditionalExpression(tree, type);
  }*/

  @Override
  public Void visitTypeCast(TypeCastTree tree, AnnotatedTypeMirror type) {
    if (!((GenericAnnotatedTypeFactory) atypeFactory).isRelevant(type)) {
      return null;
    }

    if (hasPrimaryAnnotationInAllHierarchies(type)) {
      // If the type is already has a primary annotation in all hierarchies, then the
      // propagated annotations won't be applied.  So don't compute them.
      log("PTA.visitTypeCast(%s, %s): hasPrimaryAnnotationInAllHierarchies%n", tree, type);
      return null;
    }

    AnnotatedTypeMirror exprType = atypeFactory.getAnnotatedType(tree.getExpression());
    if (type.getKind() == TypeKind.TYPEVAR) {
      if (exprType.getKind() == TypeKind.TYPEVAR) {
        // If both types are type variables, take the direct annotations.
        type.addMissingAnnotations(exprType.getPrimaryAnnotations());
      }
      // else do nothing.
    } else {
      // Use effective annotations from the expression, to get upper bound of type variables.
      AnnotationMirrorSet expressionAnnos = exprType.getEffectiveAnnotations();
      log(
          "PTA.visitTypeCast(%s, %s): getEffectiveAnnotations(%s) = %s%n",
          tree, type, exprType, expressionAnnos);

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
    log("addAnnoOrBound(%s, %s)%n", type, annos);
    TypeMirror tm = type.getUnderlyingType();
    AnnotationMirrorSet boundAnnos = atypeFactory.getQualifierUpperBounds().getBoundQualifiers(tm);
    AnnotationMirrorSet annosToAdd = new AnnotationMirrorSet();
    for (AnnotationMirror boundAnno : boundAnnos) {
      AnnotationMirror anno = qualHierarchy.findAnnotationInSameHierarchy(annos, boundAnno);
      if (anno != null && !qualHierarchy.isSubtypeShallow(anno, tm, boundAnno, tm)) {
        annosToAdd.add(boundAnno);
      }
    }
    type.addMissingAnnotations(annosToAdd);
    log("addAnnoOrBound#2(%s, %s)%n", type, annos);
    type.addMissingAnnotations(annos);
    log("addAnnoOrBound#3(%s, %s)%n", type, annos);
  }
}
