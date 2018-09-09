package org.checkerframework.checker.determinism;

import java.util.Map;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.poly.DefaultQualifierPolymorphism;
import org.checkerframework.framework.util.AnnotationMirrorMap;
import org.checkerframework.framework.util.AnnotationMirrorSet;
import org.checkerframework.javacutil.TypesUtils;

// TODO: I don't see how the least upper bound of argument types is relevant.  The point is to find
// the lub of the actual argument types that correspond to all the occurrences of @PolyDet within
// the method signature (whether on formal parameters or elsewhere, such as on parameter
// elements/components).  This is both less than all arguments (it's only those annotated as
// @PolyDet) and more than all arguments (@PolyDet may appear elsewhere).
/**
 * Resolves polymorphic annotations at method invocations as follows:
 *
 * <ol>
 *   <li>Resolves a type annotated as {@code @PolyDet("up")} to {@code @NonDet} if the least upper
 *       bound of argument types resolves to {@code OrderNonDet}.
 *   <li>Resolves a type annotated as {@code @PolyDet("down")} to {@code @Det} if the least upper
 *       bound of argument types resolves to {@code OrderNonDet}.
 *   <li>Resolves a type annotated as {@code @PolyDet("use")} to the same annotation that
 *       {@code @PolyDet} resolves to for the other arguments.
 * </ol>
 */
public class DeterminismQualifierPolymorphism extends DefaultQualifierPolymorphism {

    /** Determinism Checker type factory. */
    DeterminismAnnotatedTypeFactory factory;

    /**
     * Creates a {@link DefaultQualifierPolymorphism} instance that uses the Determinism Checker for
     * querying type qualifiers and the {@link DeterminismAnnotatedTypeFactory} for getting
     * annotated types.
     *
     * @param env the processing environment
     * @param factory the type factory for the Determinism Checker
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

        if (type.hasAnnotation(factory.POLYDET) || type.hasAnnotation(factory.POLYDET_USE)) {
            // TODO: This code assumes that `replacements` contains exactly one element.  That
            // requirement is not documented in the method documentation, nor is there any defensive
            // check in the method body.  Furthermore, it is brittle and likely to break in
            // hard-to-debug ways if that assumption changes in the future.  There should be a
            // better way to make this work even if replacements does not have size 1.  (If
            // replacements really does always have size 1, why is it a map rather than just an
            // AnnotationMirror?)
            Map.Entry<AnnotationMirror, AnnotationMirrorSet> pqentry =
                    replacements.entrySet().iterator().next();
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

    // TOOD: The documentation doesn't say anything about his being a deep replacement, but the
    // implementation calls recursiveReplaceAnnotation.  It's important to make the documentation
    // complete so that readers can understand it.
    /**
     * If {@code type} has the annotation {@code @OrderNonDet}, this method replaces the annotation
     * of {@code type} with {@code replaceType}.
     *
     * @param type the polymorphic type to be replaced
     * @param replaceType the type to be replaced with
     */
    private void replaceOrderNonDet(AnnotatedTypeMirror type, AnnotationMirror replaceType) {
        if (!type.hasAnnotation(factory.ORDERNONDET)) {
            return;
        }

        type.replaceAnnotation(replaceType);

        // This check succeeds for @OrderNonDet Set<T> (Generic types)
        if (TypesUtils.getTypeElement(type.getUnderlyingType()) == null) {
            return;
        }

        // TODO-rashmi: Handle Maps
        recursiveReplaceAnnotation(type, replaceType);
    }

    // TODO: One example is not enough here.  Also give one where there is a deeper replacement, and
    // one where a replacement does not occur.
    /**
     * Iterates over all the nested Collection/Iterator type arguments of {@code type} and replaces
     * their top-level annotations with {@code replaceType} if these top-level annotations are
     * {@code OrderNonDet}.
     *
     * <p>Example: If this method is called with {@code type} as {@code @OrderNonDet
     * Set<@OrderNonDet Set<@Det Integer>>} and {@code replaceType} as {@code @NonDet}, the result
     * will be {@code @NonDet Set<@NonDet Set<@Det Integer>>}.
     */
    void recursiveReplaceAnnotation(AnnotatedTypeMirror type, AnnotationMirror replaceType) {
        TypeMirror underlyingTypeOfReceiver =
                TypesUtils.getTypeElement(type.getUnderlyingType()).asType();
        // What if there is a user-defined collection, such as Box or Cell?
        // The manual should document what the checker does in that case.
        // What if the user writes the type Box<@OrderNonDet Integer>?
        if (!(factory.isCollection(underlyingTypeOfReceiver)
                || factory.isIterator(underlyingTypeOfReceiver))) {
            return;
        }

        AnnotatedDeclaredType declaredTypeOuter = (AnnotatedDeclaredType) type;
        AnnotatedTypeMirror argType = declaredTypeOuter.getTypeArguments().get(0);
        if (argType.hasAnnotation(factory.ORDERNONDET)) {
            argType.replaceAnnotation(replaceType);
        }
        recursiveReplaceAnnotation(argType, replaceType);
    }
}
