package org.checkerframework.framework.type;

import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.util.AnnotatedTypes;
import org.checkerframework.javacutil.ErrorReporter;

/**
 * Utility class for applying the annotations inferred by dataflow to a given type.
 */
public class DefaultInferredTypesApplier {

    // At the moment, only Inference uses the omitSubtypingCheck option.
    // In actuality the subtyping check should be unnecessary since inferred
    // types should be subtypes of their declaration.
    private final boolean omitSubtypingCheck;

    private final QualifierHierarchy hierarchy;
    private final AnnotatedTypeFactory factory;

    public DefaultInferredTypesApplier(QualifierHierarchy hierarchy, AnnotatedTypeFactory factory) {
        this(false, hierarchy, factory);
    }

    public DefaultInferredTypesApplier(
            boolean omitSubtypingCheck,
            QualifierHierarchy hierarchy,
            AnnotatedTypeFactory factory) {
        this.omitSubtypingCheck = omitSubtypingCheck;
        this.hierarchy = hierarchy;
        this.factory = factory;
    }

    /**
     * For each top in qualifier hierarchy, traverse inferred and copy the required annotations over to
     * type.
     * @param type the type to which annotations are being applied
     * @param inferredSet the type inferred by data flow
     * @param inferredTypeMirror underlying inferred type
     */
    public void applyInferredType(
            final AnnotatedTypeMirror type,
            final Set<AnnotationMirror> inferredSet,
            final TypeMirror inferredTypeMirror) {
        if (inferredSet == null) {
            return;
        }
        for (final AnnotationMirror top : hierarchy.getTopAnnotations()) {
            AnnotationMirror inferred = hierarchy.findAnnotationInHierarchy(inferredSet, top);

            apply(type, inferred, inferredTypeMirror, top);
        }
    }

    private void apply(
            AnnotatedTypeMirror type,
            AnnotationMirror inferred,
            TypeMirror inferredTypeMirror,
            AnnotationMirror top) {

        AnnotationMirror primary = type.getAnnotationInHierarchy(top);
        if (inferred == null && primary == null) {
            // Type doesn't have a primary either, nothing to remove
            return;
        } else if (inferred == null && type.getKind() == TypeKind.TYPEVAR) {
            removePrimaryAnnotationTypeVar(type, inferredTypeMirror, top, primary);
        } else if (inferred == null) {
            removePrimaryTypeVarApplyUpperBound(type, inferredTypeMirror, top, primary);
        } else if (primary != null
                && (omitSubtypingCheck || hierarchy.isSubtype(inferred, primary))) {
            type.replaceAnnotation(inferred);
        } else if (primary == null) {
            Set<AnnotationMirror> lowerbounds =
                    AnnotatedTypes.findEffectiveLowerBoundAnnotations(hierarchy, type);
            AnnotationMirror lowerbound = hierarchy.findAnnotationInHierarchy(lowerbounds, top);
            if (omitSubtypingCheck || hierarchy.isSubtype(inferred, lowerbound)) {
                type.replaceAnnotation(inferred);
            }
        }
    }

    private void removePrimaryTypeVarApplyUpperBound(
            AnnotatedTypeMirror type,
            TypeMirror inferredTypeMirror,
            AnnotationMirror top,
            AnnotationMirror notInferred) {
        if (inferredTypeMirror.getKind() != TypeKind.TYPEVAR) {
            ErrorReporter.errorAbort(
                    "Inferred value should not be missing annotations: " + inferredTypeMirror);
            return;
        }

        TypeVariable typeVar = (TypeVariable) inferredTypeMirror;
        AnnotatedTypeVariable typeVariableDecl =
                (AnnotatedTypeVariable) factory.getAnnotatedType(typeVar.asElement());
        AnnotationMirror upperBound = typeVariableDecl.getEffectiveAnnotationInHierarchy(top);

        if (omitSubtypingCheck || hierarchy.isSubtype(upperBound, notInferred)) {
            type.replaceAnnotation(upperBound);
        }
    }

    private void removePrimaryAnnotationTypeVar(
            AnnotatedTypeMirror type,
            TypeMirror typeMirror,
            AnnotationMirror top,
            final AnnotationMirror notInferred) {
        if (typeMirror.getKind() != TypeKind.TYPEVAR) {
            ErrorReporter.errorAbort("Missing annos");
            return;
        }
        TypeVariable typeVar = (TypeVariable) typeMirror;
        AnnotatedTypeVariable typeVariableDecl =
                (AnnotatedTypeVariable) factory.getAnnotatedType(typeVar.asElement());
        AnnotationMirror upperBound = typeVariableDecl.getEffectiveAnnotationInHierarchy(top);
        if (omitSubtypingCheck || hierarchy.isSubtype(upperBound, notInferred)) {
            AnnotatedTypeVariable typeTV = (AnnotatedTypeVariable) type;
            type.removeAnnotationInHierarchy(top);

            AnnotationMirror ub = typeVariableDecl.getUpperBound().getAnnotationInHierarchy(top);
            apply(typeTV.getUpperBound(), ub, typeVar.getUpperBound(), top);
            AnnotationMirror lb = typeVariableDecl.getLowerBound().getAnnotationInHierarchy(top);
            apply(typeTV.getLowerBound(), lb, typeVar.getLowerBound(), top);
        }
    }
}
