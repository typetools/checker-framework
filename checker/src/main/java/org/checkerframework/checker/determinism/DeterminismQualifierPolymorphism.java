package org.checkerframework.checker.determinism;

import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import java.util.*;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.util.Elements;
import org.checkerframework.checker.determinism.qual.NonDet;
import org.checkerframework.checker.determinism.qual.PolyDet;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.poly.AbstractQualifierPolymorphism;
import org.checkerframework.framework.util.AnnotationMirrorMap;
import org.checkerframework.framework.util.AnnotationMirrorSet;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;

public class DeterminismQualifierPolymorphism extends AbstractQualifierPolymorphism {

    Elements elements;
    ProcessingEnvironment env;
    DeterminismAnnotatedTypeFactory factory;
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
        polyQuals.put(
                AnnotationBuilder.fromClass(elements, PolyDet.class),
                AnnotationBuilder.fromClass(elements, NonDet.class));
    }

    @Override
    protected void replace(
            AnnotatedTypeMirror type, AnnotationMirrorMap<AnnotationMirrorSet> matches) {
        String upOrDown = "";
        if (type.getAnnotations().size() != 0) {
            AnnotationMirror typeAnnoMirror = type.getAnnotations().iterator().next();
            boolean hasValue = AnnotationUtils.hasElementValue(typeAnnoMirror, "value");
            if (hasValue) {
                upOrDown =
                        AnnotationUtils.getElementValue(
                                typeAnnoMirror, "value", String.class, true);
                type.replaceAnnotation(AnnotationBuilder.fromClass(elements, PolyDet.class));
            }
        }
        for (Map.Entry<AnnotationMirror, AnnotationMirrorSet> pqentry : matches.entrySet()) {
            AnnotationMirror poly = pqentry.getKey();
            if (poly != null && type.hasAnnotation(poly)) {
                type.removeAnnotation(poly);
                AnnotationMirrorSet quals = pqentry.getValue();
                type.replaceAnnotations(quals);
                if (type.hasAnnotation(factory.ORDERNONDET)) {
                    if (upOrDown.equals("down")) type.replaceAnnotation(factory.DET);
                    else if (upOrDown.equals("up")) type.replaceAnnotation(factory.NONDET);
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

    @Override
    public void annotate(NewClassTree tree, AnnotatedTypeMirror.AnnotatedExecutableType type) {
        super.annotate(tree, type);
    }

    @Override
    public void annotate(
            MethodInvocationTree tree, AnnotatedTypeMirror.AnnotatedExecutableType type) {
        super.annotate(tree, type);
    }
}
