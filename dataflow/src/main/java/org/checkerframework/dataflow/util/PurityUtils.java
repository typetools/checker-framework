package org.checkerframework.dataflow.util;

import static org.checkerframework.dataflow.qual.Pure.Kind.DETERMINISTIC;
import static org.checkerframework.dataflow.qual.Pure.Kind.SIDE_EFFECT_FREE;

import com.sun.source.tree.MethodTree;
import java.util.EnumSet;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import org.checkerframework.dataflow.qual.Deterministic;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.javacutil.AnnotationProvider;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.TreeUtils;

/**
 * An utility class for working with the {@link SideEffectFree}, {@link Deterministic}, and {@link
 * Pure} annotations.
 *
 * @see SideEffectFree
 * @see Deterministic
 * @see Pure
 */
public class PurityUtils {

    /**
     * Does the method {@code tree} have any purity annotation?
     *
     * @param provider how to get annotations
     * @param tree a method to test
     * @return whether the method has any purity annotations
     */
    public static boolean hasPurityAnnotation(AnnotationProvider provider, MethodTree tree) {
        return !getPurityKinds(provider, tree).isEmpty();
    }

    /**
     * Does the method {@code methodElement} have any purity annotation?
     *
     * @param provider how to get annotations
     * @param methodElement a method to test
     * @return whether the method has any purity annotations
     */
    public static boolean hasPurityAnnotation(AnnotationProvider provider, Element methodElement) {
        return !getPurityKinds(provider, methodElement).isEmpty();
    }

    /**
     * Is the method {@code tree} deterministic?
     *
     * @param provider how to get annotations
     * @param tree a method to test
     * @return whether the method is deterministic
     */
    public static boolean isDeterministic(AnnotationProvider provider, MethodTree tree) {
        Element methodElement = TreeUtils.elementFromTree(tree);
        if (methodElement == null) {
            throw new BugInCF("Could not find element for tree: " + tree);
        }
        return isDeterministic(provider, methodElement);
    }

    /**
     * Is the method {@code methodElement} deterministic?
     *
     * @param provider how to get annotations
     * @param methodElement a method to test
     * @return whether the method is deterministic
     */
    public static boolean isDeterministic(AnnotationProvider provider, Element methodElement) {
        EnumSet<Pure.Kind> kinds = getPurityKinds(provider, methodElement);
        return kinds.contains(DETERMINISTIC);
    }

    /**
     * Is the method {@code tree} side-effect-free?
     *
     * @param provider how to get annotations
     * @param tree a method to test
     * @return whether the method is side-effect-free
     */
    public static boolean isSideEffectFree(AnnotationProvider provider, MethodTree tree) {
        Element methodElement = TreeUtils.elementFromTree(tree);
        if (methodElement == null) {
            throw new BugInCF("Could not find element for tree: " + tree);
        }
        return isSideEffectFree(provider, methodElement);
    }

    /**
     * Is the method {@code methodElement} side-effect-free?
     *
     * @param provider how to get annotations
     * @param methodElement a method to test
     * @return whether the method is side-effect-free
     */
    public static boolean isSideEffectFree(AnnotationProvider provider, Element methodElement) {
        EnumSet<Pure.Kind> kinds = getPurityKinds(provider, methodElement);
        return kinds.contains(SIDE_EFFECT_FREE);
    }

    /**
     * @param provider how to get annotations
     * @param tree a method to test
     * @return the types of purity of the method {@code tree}.
     */
    public static EnumSet<Pure.Kind> getPurityKinds(AnnotationProvider provider, MethodTree tree) {
        Element methodElement = TreeUtils.elementFromTree(tree);
        if (methodElement == null) {
            throw new BugInCF("Could not find element for tree: " + tree);
        }
        return getPurityKinds(provider, methodElement);
    }

    /**
     * @param provider how to get annotations
     * @param methodElement a method to test
     * @return the types of purity of the method {@code methodElement}. TODO: should the return type
     *     be an EnumSet?
     */
    public static EnumSet<Pure.Kind> getPurityKinds(
            AnnotationProvider provider, Element methodElement) {
        AnnotationMirror pureAnnotation = provider.getDeclAnnotation(methodElement, Pure.class);
        AnnotationMirror sefAnnotation =
                provider.getDeclAnnotation(methodElement, SideEffectFree.class);
        AnnotationMirror detAnnotation =
                provider.getDeclAnnotation(methodElement, Deterministic.class);

        if (pureAnnotation != null) {
            return EnumSet.of(DETERMINISTIC, SIDE_EFFECT_FREE);
        }
        EnumSet<Pure.Kind> kinds = EnumSet.noneOf(Pure.Kind.class);
        if (sefAnnotation != null) {
            kinds.add(SIDE_EFFECT_FREE);
        }
        if (detAnnotation != null) {
            kinds.add(DETERMINISTIC);
        }
        return kinds;
    }
}
