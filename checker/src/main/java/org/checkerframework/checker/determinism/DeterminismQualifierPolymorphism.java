package org.checkerframework.checker.determinism;

import java.util.*;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.poly.DefaultQualifierPolymorphism;
import org.checkerframework.framework.util.AnnotationMirrorMap;
import org.checkerframework.framework.util.AnnotationMirrorSet;
import org.checkerframework.javacutil.TypesUtils;

/** Resolves polymorphic annotations for the determinism type-system. */
public class DeterminismQualifierPolymorphism extends DefaultQualifierPolymorphism {

    Elements elements;
    ProcessingEnvironment env;
    DeterminismAnnotatedTypeFactory factory;
    AnnotationMirror POLYDET;
    AnnotationMirror POLYDET_USE;
    AnnotationMirror POLYDET_UP;
    AnnotationMirror POLYDET_DOWN;

    /**
     * Creates a {@link DefaultQualifierPolymorphism} instance that uses the determinism checker for
     * querying type qualifiers and the {@link DeterminismAnnotatedTypeFactory} for getting
     * annotated types.
     *
     * @param env the processing environment
     * @param factory the factory for the current checker
     */
    public DeterminismQualifierPolymorphism(
            ProcessingEnvironment env, DeterminismAnnotatedTypeFactory factory) {
        super(env, factory);
        this.env = env;
        this.factory = factory;
        POLYDET = factory.POLYDET;
        POLYDET_USE = factory.POLYDET_USE;
        POLYDET_UP = factory.POLYDET_UP;
        POLYDET_DOWN = factory.POLYDET_DOWN;
    }

    /**
     * Replaces {@code @PolyDet("up")} with{@code @NonDet} if it resolves to {@code OrderNonDet}.
     * Replaces {@code @PolyDet("down")} with{@code @Det} if it resolves to {@code OrderNonDet}.
     * Replaces {@code @PolyDet("use")} with the same annotation that {@code @PolyDet} resolves to.
     *
     * @param type The polymorphic type to be replaced
     * @param matches The Set of AnnotationMirrors that can replace 'type'
     */
    @Override
    protected void replace(
            AnnotatedTypeMirror type, AnnotationMirrorMap<AnnotationMirrorSet> matches) {
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
            if (poly != null && (type.hasAnnotation(poly) || type.hasAnnotation(POLYDET_USE))) {
                type.removeAnnotation(poly);
                AnnotationMirrorSet quals = pqentry.getValue();
                type.replaceAnnotations(quals);

                if (type.hasAnnotation(factory.ORDERNONDET)) {
                    if (polyUp) {
                        replaceOrderNonDet(type, factory.NONDET);
                    }
                    if (polyDown) {
                        replaceOrderNonDet(type, factory.DET);
                    }
                }
            }
        }
    }

    /**
     * Helper method that replaces {@code @OrderNonDet} with either {@code @Det} (in case of
     * {@code @PolyDet("up")}) or {@code @NonDet} (in case of {@code @PolyDet("down")}).
     *
     * @param type The polymorphic type to be replaced
     * @param replaceType The type to be replaced with
     */
    private void replaceOrderNonDet(AnnotatedTypeMirror type, AnnotationMirror replaceType) {
        TypeMirror underlyingType = TypesUtils.getTypeElement(type.getUnderlyingType()).asType();
        type.replaceAnnotation(replaceType);
        AnnotatedTypeMirror.AnnotatedDeclaredType declaredType = null;
        boolean isCollIter = false;
        if (factory.isCollection(underlyingType) || factory.isIterator(underlyingType)) {
            declaredType = (AnnotatedTypeMirror.AnnotatedDeclaredType) type;
            isCollIter = true;
        }
        while (isCollIter) {
            Iterator<AnnotatedTypeMirror> it = declaredType.getTypeArguments().iterator();
            // Iterate over all the type parameters of this collection and
            // replace all @OrderNonDet type parameters with 'replaceType'.
            while (it.hasNext()) {
                AnnotatedTypeMirror argType = it.next();
                if (argType.hasAnnotation(factory.ORDERNONDET)) {
                    argType.replaceAnnotation(replaceType);
                }
            }

            // Assuming a single type parameter (will not work for HashMaps)
            // TODO: Handle all type parameters
            TypeMirror declType =
                    TypesUtils.getTypeElement(
                                    declaredType.getTypeArguments().get(0).getUnderlyingType())
                            .asType();
            if (factory.isCollection(declType) || factory.isIterator(declType)) {
                declaredType =
                        (AnnotatedTypeMirror.AnnotatedDeclaredType)
                                declaredType.getTypeArguments().get(0);
            } else {
                isCollIter = false;
            }
        }
    }
}
