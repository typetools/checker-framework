package org.checkerframework.framework.type.poly;

import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.Name;
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

    /** @return true if {@code qual} is {@code @PolyAll} */
    static boolean isPolyAll(AnnotationMirror qual) {
        return AnnotationUtils.areSameByClass(qual, PolyAll.class);
    }

    /**
     * @return the {@link PolymorphicQualifier} meta-annotation on {@code qual} if one exists;
     *     otherwise return null
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

    /**
     * @return true if {@code qual} has the {@link PolymorphicQualifier} meta-annotation
     * @deprecated use {@link #hasPolymorphicQualifier}
     */
    @Deprecated // use hasPolymorphicQualifier()
    static boolean isPolymorphicQualified(AnnotationMirror qual) {
        return getPolymorphicQualifier(qual) != null;
    }

    /** @return true if {@code qual} has the {@link PolymorphicQualifier} meta-annotation. */
    static boolean hasPolymorphicQualifier(AnnotationMirror qual) {
        return getPolymorphicQualifier(qual) != null;
    }

    /**
     * If {@code qual} is a polymorphic qualifier, return the class specified by the {@link
     * PolymorphicQualifier} meta-annotation on the polymorphic qualifier is returned. Otherwise,
     * return null.
     *
     * <p>This value identifies the qualifier hierarchy to which this polymorphic qualifier belongs.
     * By convention, it is the top qualifier of the hierarchy. Use of {@code
     * PolymorphicQualifier.class} is discouraged, because it can lead to ambiguity if used for
     * multiple type systems.
     *
     * @param qual an annotation
     * @return the class specified by the {@link PolymorphicQualifier} meta-annotation on {@code
     *     qual}, if {@code qual} is a polymorphic qualifier; otherwise, null.
     * @see org.checkerframework.framework.qual.PolymorphicQualifier#value()
     * @deprecated use {@link getPolymorphicQualifierElement}
     */
    @Deprecated // use getPolymorphicQualifierElement()
    static Name getPolymorphicQualifierTop(AnnotationMirror qual) {
        AnnotationMirror poly = getPolymorphicQualifier(qual);

        // System.out.println("poly: " + poly + " pq: " +
        //     PolymorphicQualifier.class.getCanonicalName());
        if (poly == null) {
            return null;
        }
        Name ret = AnnotationUtils.getElementValueClassName(poly, "value", true);
        return ret;
    }

    /**
     * If {@code qual} is a polymorphic qualifier, return the class specified by the {@link
     * PolymorphicQualifier} meta-annotation on the polymorphic qualifier is returned. Otherwise,
     * return null.
     *
     * <p>This value identifies the qualifier hierarchy to which this polymorphic qualifier belongs.
     * By convention, it is the top qualifier of the hierarchy. Use of {@code
     * PolymorphicQualifier.class} is discouraged, because it can lead to ambiguity if used for
     * multiple type systems.
     *
     * @param qual an annotation
     * @return the class specified by the {@link PolymorphicQualifier} meta-annotation on {@code
     *     qual}, if {@code qual} is a polymorphic qualifier; otherwise, null.
     * @see org.checkerframework.framework.qual.PolymorphicQualifier#value()
     */
    static Name getPolymorphicQualifierElement(AnnotationMirror qual) {
        AnnotationMirror poly = getPolymorphicQualifier(qual);

        // System.out.println("poly: " + poly + " pq: " +
        //     PolymorphicQualifier.class.getCanonicalName());
        if (poly == null) {
            return null;
        }
        Name ret = AnnotationUtils.getElementValueClassName(poly, "value", true);
        return ret;
    }

    /**
     * Resolves polymorphism annotations for the given type.
     *
     * @param tree the tree associated with the type
     * @param type the type to annotate; is side-effected by this method
     */
    void annotate(MethodInvocationTree tree, AnnotatedExecutableType type);

    /**
     * Resolves polymorphism annotations for the given type.
     *
     * @param tree the tree associated with the type
     * @param type the type to annotate; is side-effected by this method
     */
    void annotate(NewClassTree tree, AnnotatedExecutableType type);

    /**
     * Resolves polymorphism annotations for the given type.
     *
     * @param functionalInterface the function type of {@code memberReference}
     * @param memberReference the type of a member reference; is side-effected by this method
     */
    void annotate(
            AnnotatedExecutableType functionalInterface, AnnotatedExecutableType memberReference);
}
