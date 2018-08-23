package org.checkerframework.checker.determinism;

import java.util.*;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.poly.DefaultQualifierPolymorphism;
import org.checkerframework.framework.util.AnnotationMirrorMap;
import org.checkerframework.framework.util.AnnotationMirrorSet;
import org.checkerframework.javacutil.TypesUtils;

/** Resolves polymorphic annotations for the determinism type-system. */
public class DeterminismQualifierPolymorphism extends DefaultQualifierPolymorphism {

    /** Determinism checker factory. */
    DeterminismAnnotatedTypeFactory factory;

    /**
     * Creates a {@link DefaultQualifierPolymorphism} instance that uses the determinism checker for
     * querying type qualifiers and the {@link DeterminismAnnotatedTypeFactory} for getting
     * annotated types.
     *
     * @param env the processing environment
     * @param factory the factory for the determinism checker
     */
    public DeterminismQualifierPolymorphism(
            ProcessingEnvironment env, DeterminismAnnotatedTypeFactory factory) {
        super(env, factory);
        this.factory = factory;
    }

    /**
     * Replaces {@code @PolyDet} in {@code type} with the instantiations in {@code matches}.
     * Replaces {@code @PolyDet("up")} with {@code @NonDet} if it resolves to {@code OrderNonDet}.
     * Replaces {@code @PolyDet("down")} with {@code @Det} if it resolves to {@code OrderNonDet}.
     * Replaces {@code @PolyDet("use")} with the same annotation that {@code @PolyDet} resolves to.
     *
     * <p>This method is called on all parts of a type.
     *
     * @param type annotated type whose poly annotations are replaced
     * @param replacements mapping from polymorphic annotation to instantiation
     */
    @Override
    protected void replace(
            AnnotatedTypeMirror type, AnnotationMirrorMap<AnnotationMirrorSet> replacements) {
        boolean polyUp = false;
        boolean polyDown = false;
        if (type.hasAnnotation(factory.POLYDET_UP)) {
            polyUp = true;
            type.replaceAnnotation(factory.POLYDET);
        } else if (type.hasAnnotation(factory.POLYDET_DOWN)) {
            polyDown = true;
            type.replaceAnnotation(factory.POLYDET);
        }
        for (Map.Entry<AnnotationMirror, AnnotationMirrorSet> pqentry : replacements.entrySet()) {
            AnnotationMirror poly = pqentry.getKey();
            if (poly != null
                    && (type.hasAnnotation(poly) || type.hasAnnotation(factory.POLYDET_USE))) {
                type.removeAnnotation(poly);
                AnnotationMirrorSet quals = pqentry.getValue();
                type.replaceAnnotations(quals);
            }
        }

        if (type.hasAnnotation(factory.ORDERNONDET)) {
            if (polyUp) {
                replaceOrderNonDet(type, factory.NONDET);
            }
            if (polyDown) {
                replaceOrderNonDet(type, factory.DET);
            }
        }
    }

    /**
     * Helper method that replaces the annotation of {@code type} with {@code replaceType}.
     *
     * @param type the polymorphic type to be replaced
     * @param replaceType the type to be replaced with
     */
    private void replaceOrderNonDet(AnnotatedTypeMirror type, AnnotationMirror replaceType) {
        type.replaceAnnotation(replaceType);

        // Outer declaration type
        AnnotatedTypeMirror.AnnotatedDeclaredType declaredTypeOuter = null;
        // This flag is true if the type is a collection or an iterator
        boolean isCollectionOrIterator = false;

        // This check succeeds for @OrderNonDet Set<T> (Generic types)
        if (TypesUtils.getTypeElement(type.getUnderlyingType()) == null) {
            return;
        }

        TypeMirror underlyingTypeOfReceiver =
                TypesUtils.getTypeElement(type.getUnderlyingType()).asType();
        if (factory.isCollection(underlyingTypeOfReceiver)
                || factory.isIterator(underlyingTypeOfReceiver)) {
            declaredTypeOuter = (AnnotatedTypeMirror.AnnotatedDeclaredType) type;
            isCollectionOrIterator = true;
        }

        // Iterates over all the nested type parameters and does the replacement.
        // Example: @OrderNonDet Set<@OrderNonDet Set<@Det Integer>>
        // This while loop iterates twice for the two @OrderNonDet Sets.
        while (isCollectionOrIterator) {
            // Iterates over all the type parameters of this collection and
            // replaces all @OrderNonDet type parameters with 'replaceType'.
            // Example: @OrderNonDet MyMap<@Det Integer, @Det Integer>
            // This loop executes twice for the two type parameters.
            for (AnnotatedTypeMirror argType : declaredTypeOuter.getTypeArguments()) {
                if (argType.hasAnnotation(factory.ORDERNONDET)) {
                    argType.replaceAnnotation(replaceType);
                }
            }

            // Assuming a single type parameter (will not work for HashMaps)
            // TODO-rashmi: Handle all type parameters

            // Example: @OrderNonDet Set<@OrderNonDet List<@Det Integer>>
            // In the first iteration of this loop, declaredTypeOuter would be @OrderNonDet Set
            // and declaredTypeInner would be @OrderNonDet List.
            // In the second iteration, declaredTypeOuter would be @OrderNonDet List
            // and declaredTypeInner would be @Det Integer.
            TypeMirror declaredTypeInner =
                    TypesUtils.getTypeElement(
                                    declaredTypeOuter.getTypeArguments().get(0).getUnderlyingType())
                            .asType();
            if (factory.isCollection(declaredTypeInner) || factory.isIterator(declaredTypeInner)) {
                declaredTypeOuter =
                        (AnnotatedTypeMirror.AnnotatedDeclaredType)
                                declaredTypeOuter.getTypeArguments().get(0);
            } else {
                isCollectionOrIterator = false;
            }
        }
    }
}
