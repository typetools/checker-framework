package org.checkerframework.framework.type;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.VariableTree;
import java.util.List;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeParameterElement;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.type.visitor.AnnotatedTypeScanner;
import org.checkerframework.framework.util.AnnotatedTypes;
import org.checkerframework.javacutil.Pair;
import org.checkerframework.javacutil.TreeUtils;

/**
 * Converts a field or methods tree into an AnnotatedTypeMirror.
 *
 * @see org.checkerframework.framework.type.TypeFromTree
 */
class TypeFromMemberVisitor extends TypeFromTreeVisitor {

    /**
     * Adds explicit annotations on type parameter declarations an their upper bounds to uses of
     * type variables.
     */
    private final TypeVarAnnotator typeVarAnnotator = new TypeVarAnnotator();

    @Override
    public AnnotatedTypeMirror visitVariable(VariableTree node, AnnotatedTypeFactory f) {
        // Create the ATM and add non-primary annotations
        // (node.getType() does not include primary annotations, those are in
        // node.getModifier()
        AnnotatedTypeMirror result = TypeFromTree.fromTypeTree(f, node.getType());

        // Add primary annotations
        List<? extends AnnotationTree> annos = node.getModifiers().getAnnotations();
        if (annos != null && !annos.isEmpty()) {
            List<AnnotationMirror> ams = TreeUtils.annotationsFromTypeAnnotationTrees(annos);
            AnnotatedTypeMirror innerType = AnnotatedTypes.innerMostType(result);
            innerType.addAnnotations(ams);
        }

        Element elt = TreeUtils.elementFromDeclaration(node);
        AnnotatedTypeMirror lambdaParamType = inferLambdaParamAnnotations(f, result, elt);
        if (lambdaParamType != null) {
            return lambdaParamType;
        }
        return result;
    }

    @Override
    public AnnotatedTypeMirror visitMethod(MethodTree node, AnnotatedTypeFactory f) {
        ExecutableElement elt = TreeUtils.elementFromDeclaration(node);

        AnnotatedExecutableType result =
                (AnnotatedExecutableType) f.toAnnotatedType(elt.asType(), false);
        result.setElement(elt);
        // Make sure the return type field gets initialized... otherwise
        // some code throws NPE. This should be cleaned up.
        result.getReturnType();

        // TODO: Needed to visit parameter types, etc.
        // It would be nicer if this didn't decode the information from the Element and
        // instead also used the Tree.
        ElementAnnotationApplier.apply(result, elt, f);
        typeVarAnnotator.visit(result, f);

        // Add primary annotations to return type
        /* This doesn't do the right thing for annotations on type variables.
        List<? extends AnnotationTree> annos = node.getModifiers().getAnnotations();
        if (annos != null
                && !annos.isEmpty()
                && result.getReturnType().getKind() != TypeKind.VOID) {
            List<AnnotationMirror> ams = TreeUtils.annotationsFromTypeAnnotationTrees(annos);
            AnnotatedTypeMirror innerType = AnnotatedTypes.innerMostType(result.getReturnType());
            innerType.addAnnotations(ams);
        }
        */

        return result;
    }

    /**
     * Annotates uses of type variables with annotation written explicitly on the type parameter
     * declaration and/or its upper bound.
     *
     * <p>For all uses, except those in a method declaration, the type of the type variable is
     * computed using {@link TypeFromTree#fromTypeTree(AnnotatedTypeFactory, Tree)}. {@link
     * TypeFromTypeTreeVisitor#forTypeVariable(AnnotatedTypeMirror, AnnotatedTypeFactory)} is called
     * for all uses of type variables and that method looks up the annotations on type parameters
     * and type parameter upper bounds from the tree. Types in method signatures aren't created
     * using TypeFromTree#fromTypeTree, so it needs special handling.
     */
    static class TypeVarAnnotator extends AnnotatedTypeScanner<Void, AnnotatedTypeFactory> {
        @Override
        public Void visitTypeVariable(AnnotatedTypeVariable type, AnnotatedTypeFactory p) {
            TypeParameterElement tpelt =
                    (TypeParameterElement) type.getUnderlyingType().asElement();

            if (type.getAnnotations().isEmpty()
                    && type.getUpperBound().getAnnotations().isEmpty()
                    && tpelt.getEnclosingElement().getKind() != ElementKind.TYPE_PARAMETER) {
                ElementAnnotationApplier.apply(type, tpelt, p);
            }
            return super.visitTypeVariable(type, p);
        }
    }

    /**
     * @return the type of the lambda parameter or null if paramElement is not a lambda parameter
     */
    private static AnnotatedTypeMirror inferLambdaParamAnnotations(
            AnnotatedTypeFactory f, AnnotatedTypeMirror lambdaParam, Element paramElement) {
        if (paramElement.getKind() != ElementKind.PARAMETER
                || f.declarationFromElement(paramElement) == null
                || f.getPath(f.declarationFromElement(paramElement)) == null
                || f.getPath(f.declarationFromElement(paramElement)).getParentPath() == null) {

            return null;
        }
        Tree declaredInTree =
                f.getPath(f.declarationFromElement(paramElement)).getParentPath().getLeaf();
        if (declaredInTree.getKind() == Kind.LAMBDA_EXPRESSION) {
            LambdaExpressionTree lambdaDecl = (LambdaExpressionTree) declaredInTree;
            int index = lambdaDecl.getParameters().indexOf(f.declarationFromElement(paramElement));
            Pair<AnnotatedDeclaredType, AnnotatedExecutableType> res =
                    f.getFnInterfaceFromTree(lambdaDecl);
            AnnotatedExecutableType functionType = res.second;
            AnnotatedTypeMirror funcTypeParam = functionType.getParameterTypes().get(index);
            if (TreeUtils.isImplicitlyTypedLambda(declaredInTree)) {
                if (f.types.isSubtype(funcTypeParam.actualType, lambdaParam.actualType)) {
                    // The Java types should be exactly the same, but because invocation type
                    // inference (#979) isn't implement, check first.
                    return AnnotatedTypes.asSuper(f, funcTypeParam, lambdaParam);
                }
                lambdaParam.addMissingAnnotations(funcTypeParam.getAnnotations());
                return lambdaParam;

            } else {
                // The lambda expression is explicitly typed, so the parameters have declared types:
                // (String s) -> ...
                // The declared type may or may not have explicit annotations.
                // If it does not have an annotation for a hierarchy, then copy the annotation from
                // the function type rather than use usual defaulting rules.
                // Note lambdaParam is a super type of funcTypeParam, so only primary annotations
                // can be copied.
                lambdaParam.addMissingAnnotations(funcTypeParam.getAnnotations());
                return lambdaParam;
            }
        }
        return null;
    }
}
