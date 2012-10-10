package dataflow.util;

import java.util.Collections;
import java.util.List;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;

import com.sun.source.tree.MethodTree;

import javacutils.AnnotationProvider;
import javacutils.AnnotationUtils;
import javacutils.InternalUtils;

import checkers.quals.Pure;
import checkers.quals.Pure.Kind;

/**
 * An utility class for working with the {@link Pure} annotation.
 * 
 * @author Stefan Heule
 * 
 */
public class PurityUtils {

    /** Does the method {@code tree} have a purity annotation? */
    public static boolean hasPurityAnnotation(AnnotationProvider provider,
            MethodTree tree) {
        return getPurityAnnotation(provider, tree) != null;
    }

    /** Does the method {@code methodElement} have a purity annotation? */
    public static boolean hasPurityAnnotation(AnnotationProvider provider,
            Element methodElement) {
        return getPurityAnnotation(provider, methodElement) != null;
    }

    /** Is the method {@code tree} deterministic? */
    public static boolean isDeterministic(AnnotationProvider provider,
            MethodTree tree) {
        Element methodElement = InternalUtils.symbol(tree);
        return isDeterministic(provider, methodElement);
    }

    /** Is the method {@code methodElement} deterministic? */
    public static boolean isDeterministic(AnnotationProvider provider,
            Element methodElement) {
        List<Kind> kinds = getPurityKinds(provider, methodElement);
        return kinds.contains(Kind.DETERMINISTIC);
    }

    /** Is the method {@code tree} side-effect free? */
    public static boolean isSideEffectFree(AnnotationProvider provider,
            MethodTree tree) {
        Element methodElement = InternalUtils.symbol(tree);
        return isSideEffectFree(provider, methodElement);
    }

    /** Is the method {@code methodElement} side-effect free? */
    public static boolean isSideEffectFree(AnnotationProvider provider,
            Element methodElement) {
        List<Kind> kinds = getPurityKinds(provider, methodElement);
        return kinds.contains(Kind.SIDE_EFFECT_FREE);
    }

    /**
     * @return The types of purity of the method {@code tree}.
     */
    public static List<Pure.Kind> getPurityKinds(AnnotationProvider provider,
            MethodTree tree) {
        Element methodElement = InternalUtils.symbol(tree);
        return getPurityKinds(provider, methodElement);
    }

    /**
     * @return The types of purity of the method {@code methodElement}.
     */
    public static List<Pure.Kind> getPurityKinds(AnnotationProvider provider,
            Element methodElement) {
        AnnotationMirror purityAnnotation = getPurityAnnotation(provider,
                methodElement);
        if (purityAnnotation == null) {
            return Collections.emptyList();
        }
        List<Pure.Kind> kinds = AnnotationUtils
                .elementValueEnumArrayWithDefaults(purityAnnotation, "value",
                        Pure.Kind.class);
        return kinds;
    }

    /**
     * @return The pure annotation for the method {@code tree} (or {@code null},
     *         if not present).
     */
    public static/* @Nullable */AnnotationMirror getPurityAnnotation(
            AnnotationProvider provider, MethodTree tree) {
        Element methodElement = InternalUtils.symbol(tree);
        return getPurityAnnotation(provider, methodElement);
    }

    /**
     * @return The pure annotation for the method {@code methodElement} (or
     *         {@code null}, if not present).
     */
    public static/* @Nullable */AnnotationMirror getPurityAnnotation(
            AnnotationProvider provider, Element methodElement) {
        AnnotationMirror pureAnnotation = provider.getDeclAnnotation(
                methodElement, Pure.class);
        return pureAnnotation;
    }
}
