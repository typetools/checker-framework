package org.checkerframework.checker.determinism;

import java.util.*;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.util.Elements;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.poly.AbstractQualifierPolymorphism;
import org.checkerframework.framework.type.poly.DefaultQualifierPolymorphism;
import org.checkerframework.framework.util.AnnotationMirrorMap;
import org.checkerframework.framework.util.AnnotationMirrorSet;

public class DeterminismQualifierPolymorphism
        extends DefaultQualifierPolymorphism { //AbstractQualifierPolymorphism {

    Elements elements;
    ProcessingEnvironment env;
    DeterminismAnnotatedTypeFactory factory;
    AnnotationMirror POLYDET_USE;
    AnnotationMirror POLYDET;
    AnnotationMirror POLYDET_UP;
    AnnotationMirror POLYDET_DOWN;
    /**
     * Creates an {@link AbstractQualifierPolymorphism} instance that uses the given checker for
     * querying type qualifiers and the given factory for getting annotated types. Subclass need to
     * add polymorphic qualifiers to {@code polyQual}.
     *
     * @param env the processing environment
     * @param factory the factory for the current checker
     */
    //    public DeterminismQualifierPolymorphism(
    //            ProcessingEnvironment env, DeterminismAnnotatedTypeFactory factory) {
    //        super(env, factory);
    //        this.env = env;
    //        this.factory = factory;
    //        elements = env.getElementUtils();
    //        POLYDET = factory.POLYDET;
    //        POLYDET_USE = factory.POLYDET_USE;
    //        polyQuals.put(POLYDET, factory.NONDET);
    //        polyQuals.put(POLYDET_USE, factory.NONDET);
    //        polyQuals.put(POLYDET_UP, factory.POLYDET_UP);
    //        polyQuals.put(POLYDET_DOWN, factory.POLYDET_DOWN);
    //    }

    public DeterminismQualifierPolymorphism(
            ProcessingEnvironment env, DeterminismAnnotatedTypeFactory factory) {
        super(env, factory);
        this.env = env;
        this.factory = factory;
        POLYDET = factory.POLYDET;
        POLYDET_USE = factory.POLYDET_USE;
        POLYDET_UP = factory.POLYDET_UP;
        POLYDET_DOWN = factory.POLYDET_DOWN;
        //        this.polyQuals.put(factory.POLYDET_USE, factory.NONDET);
    }

    //    @Override
    //    protected void replace(
    //            AnnotatedTypeMirror type, AnnotationMirrorMap<AnnotationMirrorSet> matches) {
    //        String upDownOrUse = "";
    //        if (type.getAnnotations().size() != 0) {
    //            AnnotationMirror typeAnnoMirror = type.getAnnotations().iterator().next();
    //            boolean hasValue = AnnotationUtils.hasElementValue(typeAnnoMirror, "value");
    //            if (hasValue) {
    //                upDownOrUse =
    //                        AnnotationUtils.getElementValue(
    //                                typeAnnoMirror, "value", String.class, true);
    //                if (upDownOrUse.equals("down") || upDownOrUse.equals("up")) {
    //                    type.replaceAnnotation(POLYDET);
    //                }
    //            }
    //        }
    //        TypeMirror underlyingType = type.getUnderlyingType();
    //        for (Map.Entry<AnnotationMirror, AnnotationMirrorSet> pqentry : matches.entrySet()) {
    //            AnnotationMirror poly = pqentry.getKey();
    //            if (poly != null && type.hasAnnotation(poly)) {
    //                AnnotationMirrorSet quals;
    //                if (AnnotationUtils.areSame(poly, POLYDET_USE)) {
    //                    quals = matches.get(POLYDET);
    //                } else {
    //                    quals = pqentry.getValue();
    //                }
    //                if (quals != null) {
    //                    type.removeAnnotation(poly);
    //                    type.replaceAnnotations(quals);
    //                    System.out.println("Replaced " + poly.getAnnotationType() + " with ");
    //                    Iterator<AnnotationMirror> it = quals.iterator();
    //                    while(it.hasNext()){
    //                        System.out.println(it.next());
    //                    }
    //                }
    //
    //                if (type.hasAnnotation(factory.ORDERNONDET)) {
    //                    if (upDownOrUse.equals("down")) type.replaceAnnotation(factory.DET);
    //                    else if (upDownOrUse.equals("up")) type.replaceAnnotation(factory.NONDET);
    //                    else if (!underlyingType.getKind().isPrimitive()
    //                            && !factory.isCollection(underlyingType)
    //                            && !factory.isIterator(underlyingType)) {
    //                        type.replaceAnnotation(factory.DET);
    //                    }
    //                }
    //            }
    //        }
    //    }

    @Override
    protected void replace(
            AnnotatedTypeMirror type, AnnotationMirrorMap<AnnotationMirrorSet> matches) {
        //        System.out.println("Replacing for type: " + type);
        boolean polyUp = false;
        boolean polyDown = false;
        if (type.hasAnnotation(POLYDET_UP)) {
            polyUp = true;
            type.replaceAnnotation(POLYDET);
        } else if (type.hasAnnotation(POLYDET_DOWN)) {
            polyDown = true;
            type.replaceAnnotation(POLYDET);
        }
        for (Map.Entry<AnnotationMirror, AnnotationMirrorSet> pqentry : matches.entrySet()) {
            AnnotationMirror poly = pqentry.getKey();
            //            System.out.println(poly.getAnnotationType() + " ==> " + pqentry.getValue().iterator().next());
            if (poly != null && (type.hasAnnotation(poly) || type.hasAnnotation(POLYDET_USE))) {
                type.removeAnnotation(poly);
                AnnotationMirrorSet quals = pqentry.getValue();
                type.replaceAnnotations(quals);

                if (polyUp && type.hasAnnotation(factory.ORDERNONDET)) {
                    type.replaceAnnotation(factory.NONDET);
                } else if (polyDown && type.hasAnnotation(factory.ORDERNONDET)) {
                    type.replaceAnnotation(factory.DET);
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
