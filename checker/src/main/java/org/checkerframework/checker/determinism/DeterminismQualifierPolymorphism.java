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

/**
 * Resolves polymorphic annotations at method invocations as follows:
 *
 * <ol>
 *   <li>Resolves the return type annotated as {@code @PolyDet("up")} to {@code @NonDet} if the
 *       least upper bound of argument types resolves to {@code OrderNonDet}.
 *   <li>Resolves the return type annotated as {@code @PolyDet("down")} to {@code @Det} if the least
 *       upper bound of argument types resolves to {@code OrderNonDet}.
 *   <li>Resolves an argument type annotated as {@code @PolyDet("use")} to the same annotation that
 *       {@code @PolyDet} resolves to for the other arguments.
 * </ol>
 */
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

    /**
     * Helper method that replaces the @OrderNonDet annotation of {@code type} with {@code
     * replaceType}.
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

        // TODO-rashmi: Handle Maps
        recursiveReplaceOrderNonDet(type, replaceType);
    }

    /**
     * Iterates over all the nested type arguments and does the replacement. Example:
     * In @OrderNonDet Set<@OrderNonDet Set<@Det Integer>>, this while loop iterates twice for the
     * two @OrderNonDet Sets.
     *
     * @param type
     * @param replaceType
     */
    void recursiveReplaceOrderNonDet(AnnotatedTypeMirror type, AnnotationMirror replaceType) {
        TypeMirror underlyingTypeOfReceiver =
                TypesUtils.getTypeElement(type.getUnderlyingType()).asType();
        if (!(factory.isCollection(underlyingTypeOfReceiver)
                || factory.isIterator(underlyingTypeOfReceiver))) {
            return;
        }

        AnnotatedTypeMirror.AnnotatedDeclaredType declaredTypeOuter =
                (AnnotatedTypeMirror.AnnotatedDeclaredType) type;
        AnnotatedTypeMirror argType = declaredTypeOuter.getTypeArguments().get(0);
        if (argType.hasAnnotation(factory.ORDERNONDET)) {
            argType.replaceAnnotation(replaceType);
        }
        recursiveReplaceOrderNonDet(argType, replaceType);
    }
}
