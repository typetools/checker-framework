package org.checkerframework.framework.type.poly;

import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.Name;
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

    /**
     * Returns the {@link PolymorphicQualifier} meta-annotation on {@code qual} if one exists;
     * otherwise return null.
     *
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
     * Returns true if {@code qual} has the {@link PolymorphicQualifier} meta-annotation.
     *
     * @param qual an annotation
     * @return true if {@code qual} has the {@link PolymorphicQualifier} meta-annotation
     */
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
    void resolve(MethodInvocationTree tree, AnnotatedExecutableType type);

    /**
     * Resolves polymorphism annotations for the given type.
     *
     * @param tree the tree associated with the type
     * @param type the type to annotate; is side-effected by this method
     */
    void resolve(NewClassTree tree, AnnotatedExecutableType type);

    /**
     * Resolves polymorphism annotations for the given type.
     *
     * @param functionalInterface the function type of {@code memberReference}
     * @param memberReference the type of a member reference; is side-effected by this method
     */
    void resolve(
            AnnotatedExecutableType functionalInterface, AnnotatedExecutableType memberReference);
}
