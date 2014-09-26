package org.checkerframework.framework.type;

import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedWildcardType;
import org.checkerframework.framework.type.visitor.AbstractAtmComboVisitor;

import javax.lang.model.element.AnnotationMirror;

/**
 * Utility class for applying the annotations inferred by dataflow to a given type.
 */
public class DefaultInferredTypesApplier {

    /**
     * For each top in qualifier hierarchy, traverse inferred and copy the required annotations over to
     * type.
     * @param qualifierHierarchy
     * @param type The type to which annotations are being applied
     * @param inferred The type inferred by data flow
     */
    public void applyInferredType(final QualifierHierarchy qualifierHierarchy,
                                         final AnnotatedTypeMirror type, final AnnotatedTypeMirror inferred) {
        final InferredTypeApplyingVisitor applier = new InferredTypeApplyingVisitor(qualifierHierarchy);
        for (final AnnotationMirror top : qualifierHierarchy.getTopAnnotations())  {
            applier.visit(type, inferred, top);
        }
    }

    /**
     * Traverses type pairs, copies the annotations from the first type to the second (inferred type)
     * Traversal is necessary to add annotations to the bounds of wildcards and type variables when the
     * type to annotate is a wildcard or type variable.
     */
    protected static class InferredTypeApplyingVisitor extends AbstractAtmComboVisitor<Void, AnnotationMirror> {

        private QualifierHierarchy qualifierHierarchy;

        public InferredTypeApplyingVisitor(QualifierHierarchy qualifierHierarchy) {
            this.qualifierHierarchy = qualifierHierarchy;
        }

        @Override
        protected String defaultErrorMessage(AnnotatedTypeMirror type, AnnotatedTypeMirror inferred, AnnotationMirror top) {
            return "applyInferredToBoundedType: Unexpected AnnotatedTypeMirror combo:\n"
                    + "type="     + type     + "\n"
                    + "inferred=" + inferred + "\n"
                    + "top="      + top      + "\n";
        }

        @Override
        protected Void defaultAction(AnnotatedTypeMirror type, AnnotatedTypeMirror inferred, AnnotationMirror top) {
            final AnnotationMirror inferredAnnotation;
            if (QualifierHierarchy.canHaveEmptyAnnotationSet(type)) {
                inferredAnnotation = inferred.getAnnotationInHierarchy(top);
            } else {
                inferredAnnotation = inferred.getEffectiveAnnotationInHierarchy(top);
            }
            if (inferredAnnotation == null) {
                // We inferred "no annotation" for this hierarchy.
                type.removeAnnotationInHierarchy(top);
            } else {
                // We inferred an annotation.
                AnnotationMirror present = type.getAnnotationInHierarchy(top);
                if (present != null) {
                    if (qualifierHierarchy.isSubtype(inferredAnnotation, present)) {
                        // TODO: why is the above check needed?
                        // Shouldn't inferred qualifiers always be
                        // subtypes?
                        type.replaceAnnotation(inferredAnnotation);
                    }
                } else {
                    type.addAnnotation(inferredAnnotation);
                }
            }

            return null;
        }

        //TODO: REMOVE
        //This is overriden so that we don't have to provide null checks on the wildcard bounds (which are sometimes null)
        @Override
        public Void visit(AnnotatedTypeMirror type1, AnnotatedTypeMirror type2, AnnotationMirror annotationMirror) {
            if (type1 != null || type2!= null) {
                super.visit(type1, type2, annotationMirror);
            }
            return null;
        }

        /**
         * For TypeVariables it is important that we both compare the primary annotation and bounds.
         * Dataflow will default local variables that are also type variables as follows:
         *
         *   <T> void method(){  @TOP T t; }
         *
         * The type @TOP T is equivalent to a type  T[ extends @TOP Object super @TOP Void ]
         * For the following method:
         *
         *   <@BOTTOM T extends @TOP Object> void method(T in_t) {
         *      T t = in_t;
         *   }
         *
         * The type of in_t is the declared type of T and it is below @TOP t, so t should be refined to the
         * declared type of T[ extends @TOP Object super @BOTTOM Void].  To do this, we must apply the
         * the annotations of both bounds to type.  This requires a traversal of type.
         */
        @Override
        public Void visitTypevar_Typevar(AnnotatedTypeVariable type, AnnotatedTypeVariable inferred, AnnotationMirror top) {
            final AnnotationMirror inferredPrimary = inferred.getAnnotationInHierarchy(top);
            if (inferredPrimary != null) {
                type.replaceAnnotation(inferredPrimary);
            } else {
                type.removeAnnotationInHierarchy(top);
                visit(type.getUpperBound(), inferred.getUpperBound(), top);
                visit(type.getLowerBound(), inferred.getLowerBound(), top);
            }
            return null;
        }

        @Override
        public Void visitTypevar_Wildcard(AnnotatedTypeVariable type, AnnotatedWildcardType inferred, AnnotationMirror top) {
            final AnnotationMirror inferredPrimary = inferred.getAnnotationInHierarchy(top);
            if (inferredPrimary != null) {
                type.replaceAnnotation(inferredPrimary);
            } else {
                type.removeAnnotationInHierarchy(top);
                visit(type.getUpperBound(), inferred.getExtendsBound(), top);
                visit(type.getLowerBound(), inferred.getSuperBound(),   top);
            }
            return null;
        }

        @Override
        public Void visitWildcard_Typevar(AnnotatedWildcardType type, AnnotatedTypeVariable inferred, AnnotationMirror top) {
            final AnnotationMirror inferredPrimary = inferred.getAnnotationInHierarchy(top);
            if (inferredPrimary != null) {
                applyPrimary(type, inferredPrimary);
            } else {
                type.removeAnnotationInHierarchy(top);
                visit(type.getExtendsBound(), inferred.getUpperBound(), top);
                visit(type.getSuperBound(),   inferred.getLowerBound(), top);
            }
            return null;
        }

        @Override
        public Void visitWildcard_Wildcard(AnnotatedWildcardType type, AnnotatedWildcardType inferred, AnnotationMirror top) {
            final AnnotationMirror inferredPrimary = inferred.getAnnotationInHierarchy(top);
            if (inferredPrimary != null) {
                applyPrimary(type, inferredPrimary);
            } else {
                type.removeAnnotationInHierarchy(top);
                visit(type.getExtendsBound(), inferred.getExtendsBound(), top);
                visit(type.getSuperBound(),   inferred.getSuperBound(), top);
            }
            return null;
        }

        protected void applyPrimary(final AnnotatedTypeMirror bound, final AnnotationMirror anno) {
            if (bound != null) {
                bound.replaceAnnotation(anno);
            }
        }
    }
}
