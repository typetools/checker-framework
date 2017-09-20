package org.checkerframework.framework.type;

/** Created by jburke on 11/20/14. */
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.VariableTree;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.javacutil.Pair;
import org.checkerframework.javacutil.TreeUtils;

/**
 * Converts a field or methods tree into an AnnotatedTypeMirror
 *
 * @see org.checkerframework.framework.type.TypeFromTree
 */
class TypeFromMemberVisitor extends TypeFromTreeVisitor {

    @Override
    public AnnotatedTypeMirror visitVariable(VariableTree node, AnnotatedTypeFactory f) {
        // Create the ATM and add non-primary annotations
        // (node.getType() does not include primary annotations, those are in
        // node.getModifier()
        AnnotatedTypeMirror result = TypeFromTree.fromTypeTree(f, node.getType());

        // Add primary annotations
        Element elt = TreeUtils.elementFromDeclaration(node);
        ElementAnnotationApplier.apply(result, elt, f);
        inferLambdaParamAnnotations(f, result, elt);
        return result;

        /* An alternative I played around with. It unfortunately
         * ignores stub files, which is not good.
        com.sun.tools.javac.code.Type undType = ((JCTree)node).type;
        AnnotatedTypeMirror result;

        if (undType != null) {
            result = f.toAnnotatedType(undType);
        } else {
            // node.getType() ignores the top-level modifiers, which are
            // in node.getModifiers()
            result = f.fromTypeTree(node.getType());
            // We still need to remove all annotations.
            // result.clearAnnotations();
        }

        // TODO: Additionally decoding should NOT be necessary.
        // However, the underlying javac Type doesn't contain
        // type argument annotations.
        Element elt = TreeUtils.elementFromDeclaration(node);
        ElementAnnotationUtils.apply(result, elt, f);

        return result;*/
    }

    @Override
    public AnnotatedTypeMirror visitMethod(MethodTree node, AnnotatedTypeFactory f) {

        ExecutableElement elt = TreeUtils.elementFromDeclaration(node);

        AnnotatedExecutableType result =
                (AnnotatedExecutableType) f.toAnnotatedType(elt.asType(), false);
        result.setElement(elt);

        ElementAnnotationApplier.apply(result, elt, f);

        return result;
    }

    private static void inferLambdaParamAnnotations(
            AnnotatedTypeFactory f, AnnotatedTypeMirror result, Element paramElement) {
        if (f.declarationFromElement(paramElement) == null
                || f.getPath(f.declarationFromElement(paramElement)) == null
                || f.getPath(f.declarationFromElement(paramElement)).getParentPath() == null) {

            return;
        }
        Tree declaredInTree =
                f.getPath(f.declarationFromElement(paramElement)).getParentPath().getLeaf();
        if (declaredInTree.getKind() == Kind.LAMBDA_EXPRESSION) {
            LambdaExpressionTree lambdaDecl = (LambdaExpressionTree) declaredInTree;
            int index = lambdaDecl.getParameters().indexOf(f.declarationFromElement(paramElement));
            Pair<AnnotatedDeclaredType, AnnotatedExecutableType> res =
                    f.getFnInterfaceFromTree(lambdaDecl);
            AnnotatedExecutableType fnMethod = res.second;
            AnnotatedTypeMirror declaredParam = fnMethod.getParameterTypes().get(index);
            // TODO: Should we infer nested types (e.g. List<@x String>)
            result.addMissingAnnotations(declaredParam.getAnnotations());
        }
    }
}
