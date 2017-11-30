package org.checkerframework.javacutil;

import com.sun.source.tree.AnnotatedTypeTree;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeParameterTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticPosition;
import java.util.List;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.WildcardType;

/*>>>
import org.checkerframework.checker.nullness.qual.*;
*/

/** Miscellaneous static utility methods. */
public class InternalUtils {

    // Class cannot be instantiated.
    private InternalUtils() {
        throw new AssertionError("Class InternalUtils cannot be instantiated.");
    }

    /**
     * Helper function to extract the javac Context from the javac processing environment.
     *
     * @param env the processing environment
     * @return the javac Context
     */
    public static Context getJavacContext(ProcessingEnvironment env) {
        return ((JavacProcessingEnvironment) env).getContext();
    }

    /**
     * Obtain the class loader for {@code clazz}. If that is not available, return the system class
     * loader.
     *
     * @param clazz the class whose class loader to find
     * @return the class loader used to {@code clazz}, or the system class loader, or null if both
     *     are unavailable
     */
    public static ClassLoader getClassLoaderForClass(Class<? extends Object> clazz) {
        ClassLoader classLoader = clazz.getClassLoader();
        return classLoader == null ? ClassLoader.getSystemClassLoader() : classLoader;
    }

    /**
     * Compares tree1 to tree2 by the position at which a diagnostic (e.g., an error message) for
     * the tree should be printed.
     */
    public static int compareDiagnosticPosition(Tree tree1, Tree tree2) {
        DiagnosticPosition pos1 = (DiagnosticPosition) tree1;
        DiagnosticPosition pos2 = (DiagnosticPosition) tree2;

        int preferred = Integer.compare(pos1.getPreferredPosition(), pos2.getPreferredPosition());
        if (preferred != 0) {
            return preferred;
        }

        return Integer.compare(pos1.getStartPosition(), pos2.getStartPosition());
    }

    /** @deprecated Use {@link TreeUtils#elementFromTree(Tree)} instead */
    @Deprecated
    public static /*@Nullable*/ Element symbol(Tree tree) {
        return TreeUtils.elementFromTree(tree);
    }

    /** @deprecated Use {@link TreeUtils#isAnonymousConstructor(MethodTree)} instead */
    @Deprecated
    public static boolean isAnonymousConstructor(final MethodTree method) {
        return TreeUtils.isAnonymousConstructor(method);
    }

    /** @deprecated Use {@link TreeUtils#constructor(NewClassTree)} instead */
    @Deprecated
    public static ExecutableElement constructor(NewClassTree tree) {
        return TreeUtils.constructor(tree);
    }

    /** @deprecated Use {@link TreeUtils#annotationsFromTypeAnnotationTrees(List)} instead */
    @Deprecated
    public static final List<AnnotationMirror> annotationsFromTypeAnnotationTrees(
            List<? extends AnnotationTree> annos) {
        return TreeUtils.annotationsFromTypeAnnotationTrees(annos);
    }

    /** @deprecated Use {@link TreeUtils#annotationFromAnnotationTree(AnnotationTree)} instead */
    @Deprecated
    public static AnnotationMirror annotationFromAnnotationTree(AnnotationTree tree) {
        return TreeUtils.annotationFromAnnotationTree(tree);
    }

    /** @deprecated Use {@link TreeUtils#annotationsFromTree(AnnotatedTypeTree)} instead */
    @Deprecated
    public static final List<? extends AnnotationMirror> annotationsFromTree(
            AnnotatedTypeTree node) {
        return TreeUtils.annotationsFromTree(node);
    }

    /** @deprecated Use {@link TreeUtils#annotationsFromTree(TypeParameterTree)} instead */
    @Deprecated
    public static final List<? extends AnnotationMirror> annotationsFromTree(
            TypeParameterTree node) {
        return TreeUtils.annotationsFromTree(node);
    }

    /** @deprecated Use {@link TreeUtils#annotationsFromArrayCreation(NewArrayTree,int)} instead */
    @Deprecated
    public static final List<? extends AnnotationMirror> annotationsFromArrayCreation(
            NewArrayTree node, int level) {
        return TreeUtils.annotationsFromArrayCreation(node, level);
    }

    /** @deprecated Use {@link TreeUtils#typeOf(Tree)} instead */
    @Deprecated
    public static TypeMirror typeOf(Tree tree) {
        return TreeUtils.typeOf(tree);
    }

    /** @deprecated Use {@link TypesUtils#isCaptured(TypeVariable)} instead */
    @Deprecated
    public static boolean isCaptured(TypeVariable typeVar) {
        return TypesUtils.isCaptured(typeVar);
    }

    /** @deprecated Use {@link TypesUtils#getCapturedWildcard(TypeVariable)} instead */
    @Deprecated
    public static WildcardType getCapturedWildcard(TypeVariable typeVar) {
        return TypesUtils.getCapturedWildcard(typeVar);
    }

    /** @deprecated Use {@link TypesUtils#isClassType(TypeMirror)} instead */
    @Deprecated
    public static boolean isClassType(TypeMirror type) {
        return TypesUtils.isClassType(type);
    }

    /**
     * @deprecated Use {@link
     *     TypesUtils#leastUpperBound(TypeMirror,TypeMirror,ProcessingEnvironment)} instead
     */
    @Deprecated
    public static TypeMirror leastUpperBound(
            ProcessingEnvironment processingEnv, TypeMirror tm1, TypeMirror tm2) {
        return TypesUtils.leastUpperBound(tm1, tm2, processingEnv);
    }

    /**
     * @deprecated Use {@link
     *     TypesUtils#greatestLowerBound(TypeMirror,TypeMirror,ProcessingEnvironment)} instead
     */
    @Deprecated
    public static TypeMirror greatestLowerBound(
            ProcessingEnvironment processingEnv, TypeMirror tm1, TypeMirror tm2) {
        return TypesUtils.greatestLowerBound(tm1, tm2, processingEnv);
    }

    /**
     * @deprecated Use {@link
     *     TypesUtils#substituteMethodReturnType(Element,TypeMirror,ProcessingEnvironment)} instead
     */
    @Deprecated
    public static TypeMirror substituteMethodReturnType(
            ProcessingEnvironment env, Element methodElement, TypeMirror substitutedReceiverType) {
        return TypesUtils.substituteMethodReturnType(methodElement, substitutedReceiverType, env);
    }

    /** @deprecated Use {@link TypesUtils#getTypeElement(TypeMirror)} instead */
    @Deprecated
    public static TypeElement getTypeElement(TypeMirror type) {
        return TypesUtils.getTypeElement(type);
    }

    /**
     * @deprecated Use {@link TypesUtils#isFunctionalInterface(TypeMirror,ProcessingEnvironment)}
     *     instead
     */
    @Deprecated
    public static boolean isFunctionalInterface(TypeMirror type, ProcessingEnvironment env) {
        return TypesUtils.isFunctionalInterface(type, env);
    }

    /** @deprecated Use {@link TreeUtils#findFunction(Tree,ProcessingEnvironment)} instead */
    @Deprecated
    public static Symbol findFunction(Tree tree, ProcessingEnvironment env) {
        return TreeUtils.findFunction(tree, env);
    }
}
