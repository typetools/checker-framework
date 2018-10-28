package org.checkerframework.framework.type.poly;

import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import java.lang.annotation.Annotation;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import org.checkerframework.framework.qual.PolyAll;
import org.checkerframework.framework.qual.PolymorphicQualifier;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.javacutil.AnnotationUtils;

/**
 * Interface to implement qualifier polymorphism.
 *
 * @see PolymorphicQualifier
 * @see AbstractQualifierPolymorphism
 * @see DefaultQualifierPolymorphism
 */
public interface QualifierPolymorphism {

    /** @return true if {@code qual} is {@code @PolyAlly} */
    static boolean isPolyAll(AnnotationMirror qual) {
        return AnnotationUtils.areSameByClass(qual, PolyAll.class);
    }

    /**
     * @return the {@link PolymorphicQualifier} meta-annotation on {@code qual} if one exists;
     *     otherwise return null.
     */
    static AnnotationMirror getPolymorphicQualifier(AnnotationMirror qual) {
        if (qual == null) {
            return null;
        }
        Element qualElt = qual.getAnnotationType().asElement();
        for (AnnotationMirror am : qualElt.getAnnotationMirrors()) {
            if (AnnotationUtils.areSameByClass(am, PolymorphicQualifier.class)) {
                return am;
            }
        }
        return null;
    }

    /** @return true if {@code qual} has the {@link PolymorphicQualifier} meta-annotation. */
    static boolean isPolymorphicQualified(AnnotationMirror qual) {
        return getPolymorphicQualifier(qual) != null;
    }

    /**
     * If {@code qual} is a polymorphic qualifier, then the class specified by the the {@link
     * PolymorphicQualifier} meta-annotation on the polymorphic qualifier is returned. Otherwise,
     * null is returned.
     *
     * @param qual
     * @return the class specified by the the {@link PolymorphicQualifier} meta-annotation on {@code
     *     qual}, if {@code qual} is a polymorphich qualifier; otherwise, null.
     */
    static Class<? extends Annotation> getPolymorphicQualifierTop(AnnotationMirror qual) {
        AnnotationMirror poly = getPolymorphicQualifier(qual);

        // System.out.println("poly: " + poly + " pq: " +
        //     PolymorphicQualifier.class.getCanonicalName());
        if (poly == null) {
            return null;
        }
        @SuppressWarnings("unchecked")
        Class<? extends Annotation> ret =
                (Class<? extends Annotation>)
                        AnnotationUtils.getElementValueClass(poly, "value", true);
        return ret;
    }

    /**
     * Resolves polymorphism annotations for the given type.
     *
     * @param tree the tree associated with the type
     * @param type the type to annotate
     */
    void annotate(MethodInvocationTree tree, AnnotatedExecutableType type);

    /**
     * Resolves polymorphism annotations for the given type.
     *
     * @param tree the tree associated with the type
     * @param type the type to annotate
     */
    void annotate(NewClassTree tree, AnnotatedExecutableType type);

    /**
     * Resolves polymorphism annotations for the given type.
     *
     * @param functionalInterface the function type of {@code memberReference}
     * @param memberReference the type of a member reference
     */
    void annotate(
            AnnotatedExecutableType functionalInterface, AnnotatedExecutableType memberReference);
}
