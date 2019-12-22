package org.checkerframework.framework.type.poly;

import java.util.Map;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.util.AnnotationMirrorMap;
import org.checkerframework.framework.util.AnnotationMirrorSet;

/**
 * Default implementation of {@link AbstractQualifierPolymorphism}. The polymorphic qualifiers for a
 * checker that uses this class are found by searching all supported qualifiers. Instantiations of a
 * polymorphic qualifier are combined using lub.
 */
public class DefaultQualifierPolymorphism extends AbstractQualifierPolymorphism {

    /**
     * Creates a {@link DefaultQualifierPolymorphism} instance that uses {@code factory} for
     * querying type qualifiers and for getting annotated types.
     *
     * @param env the processing environment
     * @param factory the factory for the current checker
     */
    public DefaultQualifierPolymorphism(ProcessingEnvironment env, AnnotatedTypeFactory factory) {
        super(env, factory);

        for (AnnotationMirror top : qualHierarchy.getTopAnnotations()) {
            AnnotationMirror poly = qualHierarchy.getPolymorphicAnnotation(top);
            if (poly != null) {
                polyQuals.put(poly, top);
            }
        }
    }

    @Override
    protected void replace(
            AnnotatedTypeMirror type, AnnotationMirrorMap<AnnotationMirrorSet> replacements) {
        for (Map.Entry<AnnotationMirror, AnnotationMirrorSet> pqentry : replacements.entrySet()) {
            AnnotationMirror poly = pqentry.getKey();
            if (type.hasAnnotation(poly)) {
                type.removeAnnotation(poly);
                AnnotationMirrorSet quals;
                if (polyBind.containsKey(poly)) {
                    quals = polyBind.get(poly);
                } else {
                    quals = pqentry.getValue();
                }
                type.replaceAnnotations(quals);
            }
        }
    }

    /**
     * Returns the lub of the two sets.
     *
     * @param polyQual polymorphic qualifier for which {@code a1Annos} and {@code a2Annos} are
     *     instantiations
     * @param a1Annos a set that is an instantiation of {@code polyQual}, or null
     * @param a2Annos a set that is an instantiation of {@code polyQual}, or null
     * @return the lub of the two sets
     */
    @Override
    protected AnnotationMirrorSet combine(
            AnnotationMirror polyQual, AnnotationMirrorSet a1Annos, AnnotationMirrorSet a2Annos) {
        if (a1Annos == null) {
            if (a2Annos == null) {
                return new AnnotationMirrorSet();
            }
            return a2Annos;
        } else if (a2Annos == null) {
            return a1Annos;
        }

        AnnotationMirrorSet lubSet = new AnnotationMirrorSet();
        for (AnnotationMirror top : topQuals) {
            AnnotationMirror a1 = qualHierarchy.findAnnotationInHierarchy(a1Annos, top);
            AnnotationMirror a2 = qualHierarchy.findAnnotationInHierarchy(a2Annos, top);
            AnnotationMirror lub = qualHierarchy.leastUpperBoundTypeVariable(a1, a2);
            if (lub != null) {
                lubSet.add(lub);
            }
        }
        return lubSet;
    }
}
