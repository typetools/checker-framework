package org.checkerframework.framework.type.poly;

import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import java.lang.annotation.Annotation;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.util.Elements;
import org.checkerframework.framework.qual.PolyAll;
import org.checkerframework.framework.qual.PolymorphicQualifier;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.javacutil.AnnotationUtils;

/** The qualifier polymorphism interface. */
public interface QualifierPolymorphism {

    /**
     * Checks if the type qualifier {@code qual} is {@code PolyAll}.
     *
     * @param qual the type qualifier to be checked
     * @return true if {@code qual} is {@code PolyAll}, false otherwise
     */
    static boolean isPolyAll(AnnotationMirror qual) {
        return AnnotationUtils.areSameByClass(qual, PolyAll.class);
    }

    /**
     * Returns the AnnotationMirror of the polymorphic qualifier if {@code qual} is polymorphic.
     *
     * @param qual the type qualifier to be checked
     * @return the AnnotationMirror of the polymorphic qualifier if {@code qual} is polymorphic,
     *     null otherwise
     */
    static AnnotationMirror getPolymorphicQualifier(AnnotationMirror qual) {
        if (qual == null) {
            return null;
        }
        Element qualElt = qual.getAnnotationType().asElement();
        for (AnnotationMirror am : qualElt.getAnnotationMirrors()) {
            if (am.getAnnotationType()
                    .toString()
                    .equals(PolymorphicQualifier.class.getCanonicalName())) {
                return am;
            }
        }
        return null;
    }

    /**
     * Checks if the type qualifier {@code qual} is polymorphic.
     *
     * @param qual the type qualifier to be checked
     * @return true if {@code qual} is polymorphic, false otherwise
     */
    static boolean isPolymorphicQualified(AnnotationMirror qual) {
        return getPolymorphicQualifier(qual) != null;
    }

    /**
     * Returns null if the qualifier is not polymorphic. Returns the (given) top of the type
     * hierarchy, in which it is polymorphic, otherwise. The top qualifier is given by the
     * programmer, so must be normalized to ensure its the real top.
     */
    static Class<? extends Annotation> getPolymorphicQualifierTop(
            Elements elements, AnnotationMirror qual) {
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
     * @param tree the MethodInvocationTree associated with the type
     * @param type the type to annotate
     */
    void annotate(MethodInvocationTree tree, AnnotatedExecutableType type);

    /**
     * Resolves polymorphism annotations for the given type.
     *
     * @param tree the NewClassTree associated with the type
     * @param type the type to annotate
     */
    void annotate(NewClassTree tree, AnnotatedExecutableType type);

    /**
     * Resolves polymorphism annotations for the given member reference.
     *
     * @param functionalInterface the AnnotatedExecutableType associated with the member reference
     * @param memberReference the member reference to annotate
     */
    void annotate(
            AnnotatedExecutableType functionalInterface, AnnotatedExecutableType memberReference);
}
