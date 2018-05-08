package org.checkerframework.checker.determinism;

import com.sun.source.tree.Tree;
import java.util.*;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.util.AnnotatedTypes;
import org.checkerframework.javacutil.AnnotationUtils;
import javax.lang.model.util.Elements;

import org.checkerframework.framework.type.poly.AbstractQualifierPolymorphism;
import org.checkerframework.framework.util.AnnotationMirrorMap;
import org.checkerframework.framework.util.AnnotationMirrorSet;

public class DeterminismQualifierPolymorphism extends AbstractQualifierPolymorphism {

    Elements elements;
    ProcessingEnvironment env;
    DeterminismAnnotatedTypeFactory factory;
    AnnotationMirror POLYDET_USE;
    AnnotationMirror POLYDET;
    /**
     * Creates an {@link AbstractQualifierPolymorphism} instance that uses the given checker for
     * querying type qualifiers and the given factory for getting annotated types. Subclass need to
     * add polymorphic qualifiers to {@code polyQual}.
     *
     * @param env the processing environment
     * @param factory the factory for the current checker
     */
    public DeterminismQualifierPolymorphism(
            ProcessingEnvironment env, DeterminismAnnotatedTypeFactory factory) {
        super(env, factory);
        this.env = env;
        this.factory = factory;
        elements = env.getElementUtils();
        POLYDET = factory.POLYDET;
        POLYDET_USE = factory.POLYDET_USE;
        polyQuals.put(POLYDET, factory.NONDET);
        polyQuals.put(POLYDET_USE, factory.NONDET);
    }

    @Override
    protected void replace(
            AnnotatedTypeMirror type, AnnotationMirrorMap<AnnotationMirrorSet> matches) {
        String upDownOrUse = "";
        if (type.getAnnotations().size() != 0) {
            AnnotationMirror typeAnnoMirror = type.getAnnotations().iterator().next();
            boolean hasValue = AnnotationUtils.hasElementValue(typeAnnoMirror, "value");
            if (hasValue) {
                upDownOrUse =
                        AnnotationUtils.getElementValue(
                                typeAnnoMirror, "value", String.class, true);
                if (upDownOrUse.equals("down") || upDownOrUse.equals("up")) {
                    type.replaceAnnotation(POLYDET);
                }
            }
        }
        for (Map.Entry<AnnotationMirror, AnnotationMirrorSet> pqentry : matches.entrySet()) {
            AnnotationMirror poly = pqentry.getKey();
            if (poly != null && type.hasAnnotation(poly)) {
                AnnotationMirrorSet quals;
                if (AnnotationUtils.areSame(poly, POLYDET_USE)) {
                    quals = matches.get(POLYDET);
                } else {
                    quals = pqentry.getValue();
                }
                type.removeAnnotation(poly);
                type.replaceAnnotations(quals);

                TypeMirror underlyingType = type.getUnderlyingType();

                if (type.hasAnnotation(factory.ORDERNONDET)) {
                    if (upDownOrUse.equals("down")) type.replaceAnnotation(factory.DET);
                    else if (upDownOrUse.equals("up")) type.replaceAnnotation(factory.NONDET);
                    else if(!underlyingType.getKind().isPrimitive() &&
                            !factory.isCollection(underlyingType)){
                        type.replaceAnnotation(factory.DET);
                    }
                }
            }
        }
    }

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
            AnnotationMirror a1 = qualhierarchy.findAnnotationInHierarchy(a1Annos, top);
            AnnotationMirror a2 = qualhierarchy.findAnnotationInHierarchy(a2Annos, top);
            AnnotationMirror lub = qualhierarchy.leastUpperBoundTypeVariable(a1, a2);
            if (lub != null) {
                lubSet.add(lub);
            }
        }
        return lubSet;
    }
}
