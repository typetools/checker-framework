package org.checkerframework.framework.type;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import java.util.Collections;
import java.util.List;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.util.AnnotatedTypes;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.TreePathUtil;
import org.checkerframework.javacutil.TreeUtils;

/**
 * Converts a field or method tree into an AnnotatedTypeMirror.
 *
 * @see org.checkerframework.framework.type.TypeFromTree
 */
class TypeFromMemberVisitor extends TypeFromTreeVisitor {

  @Override
  public AnnotatedTypeMirror visitVariable(VariableTree variableTree, AnnotatedTypeFactory f) {
    Element elt = TreeUtils.elementFromDeclaration(variableTree);

    // Create the ATM and add non-primary annotations
    AnnotatedTypeMirror result;
    // Propagate initializer annotated type to variable if declared using var.
    // Skip propagation of annotations when initializer can be null.
    // E.g.
    // for (var i : list) {}
    if (TreeUtils.isVariableTreeDeclaredUsingVar(variableTree)
        && variableTree.getInitializer() != null) {
      result = f.getAnnotatedType(variableTree.getInitializer());
      // Let normal defaulting happen for the primary annotation.
      result.clearPrimaryAnnotations();
    } else if (TreeUtils.isVariableTreeDeclaredUsingVar(variableTree)
        || variableTree.getType() == null) {
      // VariableTree#getType returns null for binding variables from a
      // DeconstructionPatternTree.
      result = f.type(variableTree);
    } else {
      // (variableTree.getType() does not include the annotation before the type, so those
      // are added to the type below).
      result = TypeFromTree.fromTypeTree(f, variableTree.getType());
    }

    // Handle any annotations in variableTree.getModifiers().
    List<AnnotationMirror> modifierAnnos;
    List<? extends AnnotationTree> annoTrees = variableTree.getModifiers().getAnnotations();
    if (annoTrees != null && !annoTrees.isEmpty()) {
      modifierAnnos = TreeUtils.annotationsFromTypeAnnotationTrees(annoTrees);
    } else {
      modifierAnnos = Collections.emptyList();
    }

    if (result.getKind() == TypeKind.DECLARED
        &&
        // Annotations on enum constants are not in the TypeMirror and always apply to the
        // innermost type, so handle them in the else block.
        elt.getKind() != ElementKind.ENUM_CONSTANT) {

      // Decode the annotations from the type mirror because the annotations are already in
      // the correct place for enclosing types.  The annotations in
      // variableTree.getModifiers()
      // might apply to the enclosing type or the type itself. For example, @Tainted
      // Outer.Inner y and @Tainted
      // Inner x.  @Tainted is stored in variableTree.getModifiers() of the variable tree
      // corresponding to both x and y, but @Tainted applies to different types.
      AnnotatedDeclaredType annotatedDeclaredType = (AnnotatedDeclaredType) result;
      // The underlying type of result does not have all annotations, but the TypeMirror of
      // variableTree.getType() does.
      // VariableTree#getType returns null for binding variables from a
      // DeconstructionPatternTree.
      if (variableTree.getType() != null
          && !TreeUtils.isVariableTreeDeclaredUsingVar(variableTree)) {
        DeclaredType declaredType = (DeclaredType) TreeUtils.typeOf(variableTree.getType());
        AnnotatedTypes.applyAnnotationsFromDeclaredType(annotatedDeclaredType, declaredType);
      }

      // Handle declaration annotations
      for (AnnotationMirror anno : modifierAnnos) {
        if (AnnotationUtils.isDeclarationAnnotation(anno)) {
          // This does not treat Checker Framework compatqual annotations differently,
          // because it's not clear whether the annotation should apply to the outermost
          // enclosing type or the innermost.
          result.addAnnotation(anno);
        }
        // If anno is not a declaration annotation, it should have been applied in the call
        // to applyAnnotationsFromDeclaredType above.
      }
    } else {
      // Add the primary annotation from the variableTree.getModifiers();
      AnnotatedTypeMirror innerType = AnnotatedTypes.innerMostType(result);
      for (AnnotationMirror anno : modifierAnnos) {
        // The code here is similar to
        // org.checkerframework.framework.util.element.ElementAnnotationUtil.addDeclarationAnnotationsFromElement.
        if (AnnotationUtils.isDeclarationAnnotation(anno)
            // Always treat Checker Framework annotations as type annotations.
            && !AnnotationUtils.annotationName(anno).startsWith("org.checkerframework")) {
          // Declaration annotations apply to the outer type.
          result.addAnnotation(anno);
        } else {
          // Type annotations apply to the innermost type.
          innerType.addAnnotation(anno);
        }
      }
    }

    AnnotatedTypeMirror lambdaParamType = inferLambdaParamAnnotations(f, elt);
    if (lambdaParamType != null) {
      return lambdaParamType;
    }
    return result;
  }

  // This method has no effect on the result of ((MethodSymbol) elt).getRawTypeAttributes().  That
  // is affected elsewhere.
  @Override
  public AnnotatedTypeMirror visitMethod(MethodTree tree, AnnotatedTypeFactory f) {
    ExecutableElement elt = TreeUtils.elementFromDeclaration(tree);

    AnnotatedExecutableType result =
        (AnnotatedExecutableType) f.toAnnotatedType(elt.asType(), false);
    result.setElement(elt);
    f.initializeAtm(result);

    // Make sure the return type field gets initialized... otherwise
    // some code throws NPE. This should be cleaned up.
    result.getReturnType();

    // TODO: Needed to visit parameter types, etc.
    // It would be nicer if this didn't decode the information from the Element and
    // instead also used the Tree. If this is implemented, then care needs to be taken to put
    // any alias declaration annotations in the correct place for return types that are arrays.
    // This would be similar to
    // org.checkerframework.framework.util.element.ElementAnnotationUtil.addDeclarationAnnotationsFromElement.
    ElementAnnotationApplier.apply(result, elt, f);
    return result;
  }

  /**
   * Returns the type of the lambda parameter, or null if paramElement is not a lambda parameter.
   *
   * @param f the annotated type factory
   * @param paramElement that might be a lambda parameter
   * @return the type of the lambda parameter, or null if paramElement is not a lambda parameter
   */
  private static @Nullable AnnotatedTypeMirror inferLambdaParamAnnotations(
      AnnotatedTypeFactory f, Element paramElement) {
    if (paramElement.getKind() != ElementKind.PARAMETER
        || f.declarationFromElement(paramElement) == null
        || f.getPath(f.declarationFromElement(paramElement)) == null
        || f.getPath(f.declarationFromElement(paramElement)).getParentPath() == null) {

      return null;
    }
    Tree declaredInTree =
        f.getPath(f.declarationFromElement(paramElement)).getParentPath().getLeaf();
    if (declaredInTree instanceof LambdaExpressionTree lambdaDecl
        && TreeUtils.isImplicitlyTypedLambda(declaredInTree)) {
      int index = lambdaDecl.getParameters().indexOf(f.declarationFromElement(paramElement));

      TreePath pathToLambda = f.getPath(lambdaDecl);
      Tree enclosingTree =
          pathToLambda == null ? null : TreePathUtil.getContextForPolyExpression(pathToLambda);
      if ((enclosingTree instanceof MethodInvocationTree || enclosingTree instanceof NewClassTree)
          && f.getTypeArgumentInference().isAnyInferenceInProgress()) {
        // Below, f.getFunctionTypeFromTree(lambdaDecl) re-derives this lambda's target type by
        // re-invoking method applicability/inference for the enclosing invocation, which in turn
        // needs the type of that invocation's receiver and arguments. If any type argument
        // inference is already running further up the call stack -- not necessarily for this
        // exact enclosing invocation, but for any invocation that the re-derivation ends up
        // revisiting while resolving those receivers/arguments (e.g. a call nested a few levels
        // out whose own inference has not returned yet) -- redoing that work here can compute a
        // different, incomplete answer than the one the in-progress inference will eventually
        // settle on. See https://github.com/typetools/checker-framework/issues/7678 and
        // https://github.com/typetools/checker-framework/issues/7698. Fall back to the real,
        // already fully-resolved Java type that javac assigned to this parameter instead.
        AnnotatedTypeMirror result = f.toAnnotatedType(paramElement.asType(), false);
        f.addDefaultAnnotations(result);
        return result;
      }

      AnnotatedExecutableType functionType = f.getFunctionTypeFromTree(lambdaDecl);
      return functionType.getParameterTypes().get(index);
    }
    return null;
  }
}
