package org.checkerframework.framework.util.typeinference8.util;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree.JCNewClass;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.ErrorReporter;
import org.checkerframework.javacutil.Pair;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypeAnnotationUtils;
import org.checkerframework.javacutil.TypesUtils;

public class InternalInferenceUtils {

    public static TypeMirror lub(
            ProcessingEnvironment processingEnv, TypeMirror tm1, TypeMirror tm2) {
        Type t1 = TypeAnnotationUtils.unannotatedType(tm1);
        Type t2 = TypeAnnotationUtils.unannotatedType(tm2);
        JavacProcessingEnvironment javacEnv = (JavacProcessingEnvironment) processingEnv;
        Types types = Types.instance(javacEnv.getContext());

        return types.lub(t1, t2);
    }

    public static TypeMirror glb(
            ProcessingEnvironment processingEnv, TypeMirror tm1, TypeMirror tm2) {
        Type t1 = TypeAnnotationUtils.unannotatedType(tm1);
        Type t2 = TypeAnnotationUtils.unannotatedType(tm2);
        JavacProcessingEnvironment javacEnv = (JavacProcessingEnvironment) processingEnv;
        Types types = Types.instance(javacEnv.getContext());

        return types.glb(t1, t2);
    }

    private static DeclaredType getReceiverType(ExpressionTree tree) {
        Tree receiverTree;
        if (tree.getKind() == Kind.NEW_CLASS) {
            receiverTree = ((NewClassTree) tree).getEnclosingExpression();
        } else {
            receiverTree = TreeUtils.getReceiverTree(tree);
        }

        if (receiverTree == null) {
            return null;
        }
        TypeMirror type = TreeUtils.typeOf(receiverTree);
        if (type.getKind() == TypeKind.TYPEVAR) {
            return (DeclaredType) ((TypeVariable) type).getUpperBound();
        }
        return type.getKind() == TypeKind.DECLARED ? (DeclaredType) type : null;
    }

    /**
     * @return ExecutableType of the method invocation or new class tree adapted to the call site.
     */
    public static ExecutableType getTypeOfMethodAdaptedToUse(
            ExpressionTree expressionTree, Java8InferenceContext context) {
        if (expressionTree.getKind() == Kind.NEW_CLASS) {
            if (!TreeUtils.isDiamondTree(expressionTree)) {
                return (ExecutableType) ((JCNewClass) expressionTree).constructorType;
            }
        } else if (expressionTree.getKind() != Kind.METHOD_INVOCATION) {
            return null;
        }
        ExecutableElement ele = (ExecutableElement) TreeUtils.elementFromUse(expressionTree);

        if (ElementUtils.isStatic(ele)) {
            return (ExecutableType) ele.asType();
        }
        DeclaredType receiverType = getReceiverType(expressionTree);

        if (receiverType == null) {
            receiverType = context.enclosingType;
        }

        while (context.types.asSuper((Type) receiverType, (Symbol) ele.getEnclosingElement())
                == null) {
            TypeMirror enclosing = receiverType.getEnclosingType();
            if (enclosing == null || enclosing.getKind() != TypeKind.DECLARED) {
                if (expressionTree.getKind() == Kind.NEW_CLASS) {
                    // No receiver for the constructor.
                    return (ExecutableType) ele.asType();
                } else {
                    ErrorReporter.errorAbort("Method not found");
                }
            }
            receiverType = (DeclaredType) enclosing;
        }
        javax.lang.model.util.Types types = context.env.getTypeUtils();
        return (ExecutableType) types.asMemberOf(receiverType, ele);
    }

    /**
     * @return a supertype of S of the form {@code G<S1, ..., Sn>} and a supertype of T of the form
     *     {@code G<T1,..., Tn>} for some generic class or interface, G. If such types exist;
     *     otherwise, null is returned.
     */
    public static Pair<TypeMirror, TypeMirror> getParameterizedSupers(
            TypeMirror s, TypeMirror t, Java8InferenceContext context) {
        // com.sun.tools.javac.comp.Infer#getParameterizedSupers
        TypeMirror lubResult = lub(context.env, t, s);
        if (!TypesUtils.isParameterizedType(lubResult)) {
            return null;
        }

        Type asSuperOfT = context.types.asSuper((Type) t, ((Type) lubResult).asElement());
        Type asSuperOfS = context.types.asSuper((Type) s, ((Type) lubResult).asElement());
        return Pair.of(asSuperOfT, asSuperOfS);
    }
}
