package org.checkerframework.checker.determinism;

import java.util.Map;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.poly.DefaultQualifierPolymorphism;
import org.checkerframework.framework.util.AnnotationMirrorMap;
import org.checkerframework.framework.util.AnnotationMirrorSet;
import org.checkerframework.javacutil.TypesUtils;

// TODO: This comment is vague.  Please make it more descriptive and concrete.
/** Resolves polymorphic annotations for the determinism type-system. */
public class DeterminismQualifierPolymorphism extends DefaultQualifierPolymorphism {

    /** Determinism Checker factory. */
    DeterminismAnnotatedTypeFactory factory;

    /**
     * Creates a {@link DefaultQualifierPolymorphism} instance that uses the Determinism Checker for
     * querying type qualifiers and the {@link DeterminismAnnotatedTypeFactory} for getting
     * annotated types.
     *
     * @param env the processing environment
     * @param factory the factory for the Determinism Checker
     */
    public DeterminismQualifierPolymorphism(
            ProcessingEnvironment env, DeterminismAnnotatedTypeFactory factory) {
        super(env, factory);
        this.factory = factory;
    }

    /**
     * Replaces {@code @PolyDet} in {@code type} with the instantiations in {@code replacements}.
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
        boolean isPolyUp = false;
        boolean isPolyDown = false;
        if (type.hasAnnotation(factory.POLYDET_UP)) {
            isPolyUp = true;
            type.replaceAnnotation(factory.POLYDET);
        } else if (type.hasAnnotation(factory.POLYDET_DOWN)) {
            isPolyDown = true;
            type.replaceAnnotation(factory.POLYDET);
        }

        Map.Entry<AnnotationMirror, AnnotationMirrorSet> pqentry =
                replacements.entrySet().iterator().next();
        AnnotationMirror poly = pqentry.getKey();
        if (type.hasAnnotation(poly) || type.hasAnnotation(factory.POLYDET_USE)) {
            AnnotationMirrorSet quals = pqentry.getValue();
            type.replaceAnnotations(quals);
        }

        if (type.hasAnnotation(factory.ORDERNONDET)) {
            if (isPolyUp) {
                replaceOrderNonDet(type, factory.NONDET);
            }
            if (isPolyDown) {
                replaceOrderNonDet(type, factory.DET);
            }
        }
    }

    // TODO: I'm confused by this method.  Its name contains OrderNonDet, but its documentation
    // doesn't mention OrderNonDet.  The documentation is also incomplete:  type gets replaced by
    // replaceType in what?  Or maybe it should be "replaces the @OrderNonDet annotation of type"?
    // Please clarify.
    /**
     * Helper method that replaces the annotation of {@code type} with {@code replaceType}.
     *
     * @param type the polymorphic type to be replaced
     * @param replaceType the type to be replaced with
     */
    private void replaceOrderNonDet(AnnotatedTypeMirror type, AnnotationMirror replaceType) {
        type.replaceAnnotation(replaceType);

        // This check succeeds for @OrderNonDet Set<T> (Generic types)
        if (TypesUtils.getTypeElement(type.getUnderlyingType()) == null) {
            return;
        }

        TypeMirror underlyingTypeOfReceiver =
                TypesUtils.getTypeElement(type.getUnderlyingType()).asType();
        // TODO: The following comment is incorrect, because isCollectionOrIterator is reassigned
        // without `type` being reassigned.  Or maybe "the type" doesn't refer to `type` but some
        // other variable such as declaredTypeOuter that the reader is supposed to guess from
        // context.  Clarify, and be specific.
        // This flag is true if the type is a collection or an iterator
        boolean isCollectionOrIterator =
                (factory.isCollection(underlyingTypeOfReceiver)
                        || factory.isIterator(underlyingTypeOfReceiver));
        // Outer declaration type
        AnnotatedTypeMirror.AnnotatedDeclaredType declaredTypeOuter =
                (isCollectionOrIterator ? (AnnotatedTypeMirror.AnnotatedDeclaredType) type : null);

        // TODO: I am still confused by this loop.
        // Why not make a recursive call to this method for each type parameter?
        // That will make it easy to accommodate multiple type parameters, too (just a for loop over
        // each one), whereas I don't see how to handle multiple type parameters with the current
        // design.
        // Iterates over all the nested type parameters and does the replacement.
        // Example: In @OrderNonDet Set<@OrderNonDet Set<@Det Integer>>,
        // this while loop iterates twice for the two @OrderNonDet Sets.
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
