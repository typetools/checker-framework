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

    // TODO: This method description seems incorrect.  The method does the given replacements, and
    // it also does what the specification says.  Write the specification to describe the
    // method's behavior, not just part of it.
    /**
     * Replaces {@code @PolyDet("up")} with {@code @NonDet} if it resolves to {@code OrderNonDet}.
     * Replaces {@code @PolyDet("down")} with {@code @Det} if it resolves to {@code OrderNonDet}.
     * Replaces {@code @PolyDet("use")} with the same annotation that {@code @PolyDet} resolves to.
     *
     * @param type the polymorphic type to be replaced
     * @param replacements the Set of AnnotationMirrors that can replace 'type'
     */
    @Override
    protected void replace(
            AnnotatedTypeMirror type, AnnotationMirrorMap<AnnotationMirrorSet> replacements) {
        boolean polyUp = false;
        boolean polyDown = false;
        if (type.hasAnnotation(POLYDET_UP)) {
            polyUp = true;
            type.replaceAnnotation(POLYDET);
        } else if (type.hasAnnotation(POLYDET_DOWN)) {
            polyDown = true;
            type.replaceAnnotation(POLYDET);
        }
        for (Map.Entry<AnnotationMirror, AnnotationMirrorSet> pqentry : replacements.entrySet()) {
            AnnotationMirror poly = pqentry.getKey();
            if (poly != null && (type.hasAnnotation(poly) || type.hasAnnotation(POLYDET_USE))) {
                type.removeAnnotation(poly);
                AnnotationMirrorSet quals = pqentry.getValue();
                type.replaceAnnotations(quals);

                // Can this be done once at the end, rather than every time through the loop?
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

    // TODO: This specification is incorrect.  The replacement isn't of @Det or @NonDet, it's
    // `replaceType`.  Maybe the specification is describing one way that clients could use this
    // method, but it's not describing this method.
    /**
     * Helper method that replaces {@code @OrderNonDet} with either {@code @Det} (in case of
     * {@code @PolyDet("up")}) or {@code @NonDet} (in case of {@code @PolyDet("down")}).
     *
     * @param type The polymorphic type to be replaced
     * @param replaceType The type to be replaced with
     */
    private void replaceOrderNonDet(AnnotatedTypeMirror type, AnnotationMirror replaceType) {
        type.replaceAnnotation(replaceType);
        // Document these two variables.  There is something tricky going on with the loop and the
        // reassignments to them, but I'm not sure what it is.  I'm also confused about the
        // relationship of variables `declaredType` and `declType` -- those should be given more
        // descriptive names and both should be documented.
        AnnotatedTypeMirror.AnnotatedDeclaredType declaredType = null;
        boolean isCollIter = false;

        // This happens for @OrderNonDet Set<T> (Generic types)
        if (TypesUtils.getTypeElement(type.getUnderlyingType()) == null) {
            return;
        }
        // TODO: What is this the underlying type for?  It's the receiver, and it would be good for
        // the variable name to reflect that.  The current name is confusing.
        TypeMirror underlyingType = TypesUtils.getTypeElement(type.getUnderlyingType()).asType();
        if (factory.isCollection(underlyingType) || factory.isIterator(underlyingType)) {
            declaredType = (AnnotatedTypeMirror.AnnotatedDeclaredType) type;
            isCollIter = true;
        }
        while (isCollIter) {
            // Iterate over all the type parameters of this collection and
            // replace all @OrderNonDet type parameters with 'replaceType'.
            for (AnnotatedTypeMirror argType : declaredType.getTypeArguments()) {
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
