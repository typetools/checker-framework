package checkers.util;

import java.util.Collections;
import java.util.List;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;

import com.sun.source.tree.MethodTree;

import checkers.quals.Pure;
import checkers.quals.Pure.Kind;
import checkers.types.AnnotatedTypeFactory;

/**
 * An utility class for working with the {@link Pure} annotation.
 * 
 * @author Stefan Heule
 * 
 */
public class PurityUtils {

    /** Does the method {@code tree} have a purity annotation? */
    public static boolean hasPurityAnnotation(AnnotatedTypeFactory factory,
            MethodTree tree) {
        return getPurityAnnotation(factory, tree) != null;
    }

    /** Does the method {@code methodElement} have a purity annotation? */
    public static boolean hasPurityAnnotation(AnnotatedTypeFactory factory,
            Element methodElement) {
        return getPurityAnnotation(factory, methodElement) != null;
    }

    /** Is the method {@code tree} deterministic? */
    public static boolean isDeterministic(AnnotatedTypeFactory factory,
            MethodTree tree) {
        Element methodElement = InternalUtils.symbol(tree);
        return isDeterministic(factory, methodElement);
    }

    /** Is the method {@code methodElement} deterministic? */
    public static boolean isDeterministic(AnnotatedTypeFactory factory,
            Element methodElement) {
        List<Kind> kinds = getPurityKinds(factory, methodElement);
        return kinds.contains(Kind.DETERMINISTIC);
    }

    /** Is the method {@code tree} side-effect free? */
    public static boolean isSideEffectFree(AnnotatedTypeFactory factory,
            MethodTree tree) {
        Element methodElement = InternalUtils.symbol(tree);
        return isSideEffectFree(factory, methodElement);
    }

    /** Is the method {@code methodElement} side-effect free? */
    public static boolean isSideEffectFree(AnnotatedTypeFactory factory,
            Element methodElement) {
        List<Kind> kinds = getPurityKinds(factory, methodElement);
        return kinds.contains(Kind.SIDE_EFFECT_FREE);
    }

    /**
     * @return The types of purity of the method {@code tree}.
     */
    public static List<Pure.Kind> getPurityKinds(AnnotatedTypeFactory factory,
            MethodTree tree) {
        Element methodElement = InternalUtils.symbol(tree);
        return getPurityKinds(factory, methodElement);
    }

    /**
     * @return The types of purity of the method {@code methodElement}.
     */
    public static List<Pure.Kind> getPurityKinds(AnnotatedTypeFactory factory,
            Element methodElement) {
        AnnotationMirror purityAnnotation = getPurityAnnotation(factory,
                methodElement);
        if (purityAnnotation == null) {
            return Collections.emptyList();
        }
        List<Pure.Kind> kinds = AnnotationUtils.getElementValueEnumArray(purityAnnotation, "value",
                        Pure.Kind.class, true);
        return kinds;
    }

    /**
     * @return The pure annotation for the method {@code tree} (or {@code null},
     *         if not present).
     */
    public static/* @Nullable */AnnotationMirror getPurityAnnotation(
            AnnotatedTypeFactory factory, MethodTree tree) {
        Element methodElement = InternalUtils.symbol(tree);
        return getPurityAnnotation(factory, methodElement);
    }

    /**
     * @return The pure annotation for the method {@code methodElement} (or
     *         {@code null}, if not present).
     */
    public static/* @Nullable */AnnotationMirror getPurityAnnotation(
            AnnotatedTypeFactory factory, Element methodElement) {
        AnnotationMirror pureAnnotation = factory.getDeclAnnotation(
                methodElement, Pure.class);
        return pureAnnotation;
    }
}
