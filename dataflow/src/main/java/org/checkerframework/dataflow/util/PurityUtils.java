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

// This class cannot be moved to the org.checkerframework.common.purity package because
// dataflow.analysis.FlowExpressions uses  this class.

/**
 * A utility class for working with the {@link SideEffectFree}, {@link Deterministic}, and {@link
 * Pure} annotations.
 *
 * @see SideEffectFree
 * @see Deterministic
 * @see Pure
 */
public class PurityUtils {

    /** Does the method {@code methodTree} have any purity annotation? */
    public static boolean hasPurityAnnotation(AnnotationProvider provider, MethodTree methodTree) {
        return !getPurityKinds(provider, methodTree).isEmpty();
    }

    /** Does the method {@code methodElement} have any purity annotation? */
    public static boolean hasPurityAnnotation(AnnotationProvider provider, Element methodElement) {
        return !getPurityKinds(provider, methodElement).isEmpty();
    }

    /** Is the method {@code methodTree} deterministic? */
    public static boolean isDeterministic(AnnotationProvider provider, MethodTree methodTree) {
        Element methodElement = TreeUtils.elementFromTree(methodTree);
        if (methodElement == null) {
            throw new BugInCF("Could not find element for tree: " + methodTree);
        }
        return isDeterministic(provider, methodElement);
    }

    /** Is the method {@code methodElement} deterministic? */
    public static boolean isDeterministic(AnnotationProvider provider, Element methodElement) {
        EnumSet<Pure.Kind> kinds = getPurityKinds(provider, methodElement);
        return kinds.contains(DETERMINISTIC);
    }

    /** Is the method {@code methodTree} side-effect-free? */
    public static boolean isSideEffectFree(AnnotationProvider provider, MethodTree methodTree) {
        Element methodElement = TreeUtils.elementFromTree(methodTree);
        if (methodElement == null) {
            throw new BugInCF("Could not find element for tree: " + methodTree);
        }
        return isSideEffectFree(provider, methodElement);
    }

    /** Is the method {@code methodElement} side-effect-free? */
    public static boolean isSideEffectFree(AnnotationProvider provider, Element methodElement) {
        EnumSet<Pure.Kind> kinds = getPurityKinds(provider, methodElement);
        return kinds.contains(SIDE_EFFECT_FREE);
    }

    /** @return the types of purity of the method {@code methodTree}. */
    public static EnumSet<Pure.Kind> getPurityKinds(
            AnnotationProvider provider, MethodTree methodTree) {
        Element methodElement = TreeUtils.elementFromTree(methodTree);
        if (methodElement == null) {
            throw new BugInCF("Could not find element for tree: " + methodTree);
        }
        return getPurityKinds(provider, methodElement);
    }

    /** @return the types of purity of the method {@code methodElement} */
    public static EnumSet<Pure.Kind> getPurityKinds(
            AnnotationProvider provider, Element methodElement) {
        AnnotationMirror pureAnnotation = provider.getDeclAnnotation(methodElement, Pure.class);
        AnnotationMirror sefAnnotation =
                provider.getDeclAnnotation(methodElement, SideEffectFree.class);
        AnnotationMirror detAnnotation =
                provider.getDeclAnnotation(methodElement, Deterministic.class);

        EnumSet<Pure.Kind> result = EnumSet.noneOf(Pure.Kind.class);
        if (pureAnnotation != null) {
            result.add(DETERMINISTIC);
            result.add(SIDE_EFFECT_FREE);
        }
        if (sefAnnotation != null) {
            result.add(SIDE_EFFECT_FREE);
        }
        if (detAnnotation != null) {
            result.add(DETERMINISTIC);
        }
        return result;
    }
}
