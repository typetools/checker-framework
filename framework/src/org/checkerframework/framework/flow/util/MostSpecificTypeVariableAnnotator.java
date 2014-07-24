package org.checkerframework.framework.flow.util;

import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedIntersectionType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedWildcardType;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.type.TypeHierarchy;
import org.checkerframework.framework.util.AtmCombo;
import org.checkerframework.javacutil.ErrorReporter;
import org.checkerframework.javacutil.InternalUtils;
import org.checkerframework.javacutil.Pair;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.util.Types;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by jburke on 7/16/14.
 */
public class MostSpecificTypeVariableAnnotator {

    public static void mostSpecificTypeVariable(final Types types,
                                                final TypeHierarchy typeHierarchy,
                                                final QualifierHierarchy qualifierHierarchy,
                                                final AnnotatedTypeMirror type1, final AnnotatedTypeMirror type2,
                                                final AnnotatedTypeVariable result) {
        for(final AnnotationMirror top : qualifierHierarchy.getTopAnnotations()) {
            if(typeHierarchy.isSubtype(type1, type2, top)) {
                annotateTypeVarResult(qualifierHierarchy, types, result, type1, top);

            } else if(typeHierarchy.isSubtype(type2, type1, top)) {
                annotateTypeVarResult(qualifierHierarchy, types, result, type2, top);
            } else {
                final Pair<AnnotatedTypeVariable, AnnotatedWildcardType> capturePair = getCapturePair(type1, type2);
                if(capturePair != null) {
                    annotateTypeVarResultWithCapture(
                            qualifierHierarchy, types, result, capturePair.first, capturePair.second, top);
                } else {
                    ErrorReporter.errorAbort(
                            "Trying to annotate using two incomparable types!\n"
                          + "type1=" + type1 + "\n"
                          + "type2=" + type2 + "\n"
                          + "result=" + result
                    );
                }
            }
        }
    }

    private static void annotateTypeVarResult(final QualifierHierarchy qualifierHierarchy,
                                              final Types types,
                                              final AnnotatedTypeVariable result,
                                              final AnnotatedTypeMirror mostSpecific,
                                              final AnnotationMirror top) {
        final AnnotatedTypeMirror source = findSourceAtm(types, qualifierHierarchy, result, mostSpecific, top, false);
        final AnnotationMirror sourcePrimaryAnno = source.getAnnotationInHierarchy(top);

        //Indicates that source is a non-primary-annotated type variable declared by the same type parameter of result
        //in this case, source is equivalent to the declared type of that type variable and we copy its bounds
        if(types.isSameType(source.getUnderlyingType(), result.getUnderlyingType()) && sourcePrimaryAnno == null) {
            final AnnotatedTypeVariable resultDecl = (AnnotatedTypeVariable) source;

            //TODO: can we just do nothing since I think only primary annotation come into play?
            //TODO: this mimics the old behavior, figure out the right behavior
            final AnnotationMirror declUpperBoundAnno = resultDecl.getUpperBound().getAnnotationInHierarchy(top);
            final AnnotationMirror declLowerBoundAnno = resultDecl.getLowerBound().getAnnotationInHierarchy(top);

            if(declUpperBoundAnno != null) {
                result.getUpperBound().addAnnotation(declUpperBoundAnno);
            }

            if(declLowerBoundAnno != null) {
                result.getLowerBound().addAnnotation(resultDecl.getLowerBound().getAnnotationInHierarchy(top));
            }
        } else {
            result.replaceAnnotation(sourcePrimaryAnno);
        }
    }

    private static void annotateTypeVarResultWithCapture(final QualifierHierarchy qualifierHierarchy,
                                                         final Types types,
                                                         final AnnotatedTypeVariable result,
                                                         final AnnotatedTypeVariable capture,
                                                         final AnnotatedWildcardType wildcard,
                                                         final AnnotationMirror top) {

        //If the primary annotation of oen of the types were a subtype of the other, than
        //mostSpecificTypeVariable would already have handled this case.  Therefore, we assume
        //that we must consider the bounds.
        //FOR NOW: Use the captured type since it should be as specific as the declaration
        annotateTypeVarResult(qualifierHierarchy, types, result, capture, top);

    }

    private static AnnotatedTypeMirror findSourceAtm(final Types types,
                                                     final QualifierHierarchy qualifierHierarchy,
                                                     final AnnotatedTypeVariable result,
                                                     final AnnotatedTypeMirror toSearch,
                                                     final AnnotationMirror top,
                                                     final boolean isCapture) {
        AnnotatedTypeMirror source = toSearch;
        while( source.getAnnotationInHierarchy(top) == null &&
               !types.isSameType(result.getUnderlyingType(), source.getUnderlyingType()) ) {

            switch(source.getKind()) {
                case TYPEVAR:
                    source = ((AnnotatedTypeVariable) source).getEffectiveUpperBound();
                    break;

                case INTERSECTION:
                    source = mostSpecificSupertype(qualifierHierarchy, (AnnotatedIntersectionType) source, top);

                    if(source == null) {
                        ErrorReporter.errorAbort("AnnotatedIntersectionType has no annotation in hierarchy"
                                + "on any of its supertypes!\n"
                                + "intersectionType=" + source);
                    }
                    break;

                default:
                    ErrorReporter.errorAbort("Unexpected AnnotatedTypeMirror with no primary annotation!"
                            + "result="   + result
                            + "toSearch=" + toSearch
                            + "top="      + top
                            + "source=" + source);
            }
        }

        return source;
    }

    private static AnnotatedTypeMirror mostSpecificSupertype(final QualifierHierarchy qualifierHierarchy,
                                                             final AnnotatedIntersectionType isect,
                                                             final AnnotationMirror top) {
        AnnotatedTypeMirror result = null;
        AnnotationMirror anno = null;
        for(final AnnotatedTypeMirror supertype : isect.directSuperTypes()) {
            final AnnotationMirror superAnno = supertype.getAnnotationInHierarchy(top);
            if(superAnno != null && (anno == null || qualifierHierarchy.isSubtype(superAnno, anno))) {
                anno = superAnno;
                result = supertype;
            }
        }

        return result;
    }

    private static Pair<AnnotatedTypeVariable, AnnotatedWildcardType> getCapturePair(final AnnotatedTypeMirror type1, final AnnotatedTypeMirror type2) {
        final AnnotatedTypeVariable potentialCapture;
        final AnnotatedWildcardType wildcard;
        switch( AtmCombo.valueOf(type1, type2) ) {
            case TYPEVAR_WILDCARD:
                potentialCapture = (AnnotatedTypeVariable) type1;
                wildcard = (AnnotatedWildcardType) type2;
                break;

            case WILDCARD_TYPEVAR:
                wildcard = (AnnotatedWildcardType) type1;
                potentialCapture = (AnnotatedTypeVariable) type2;
                break;

            default:
                return null;
        }

        if(InternalUtils.isCaptured(potentialCapture.getUnderlyingType())) {
            return Pair.of(potentialCapture, wildcard);
        }

        return null;
    }
}