package org.checkerframework.framework.flow;

/*>>>
import org.checkerframework.checker.nullness.qual.Nullable;
*/

import org.checkerframework.dataflow.analysis.AbstractValue;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.dataflow.util.HashCodeUtils;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedIntersectionType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedWildcardType;
import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.type.TypeHierarchy;
import org.checkerframework.framework.type.visitor.AnnotatedTypeMerger;
import org.checkerframework.framework.util.AnnotatedTypes;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.ErrorReporter;
import org.checkerframework.javacutil.InternalUtils;

import java.util.Collection;
import java.util.Set;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.IntersectionType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;

/**
 * An implementation of an abstract value used by the Checker Framework org.checkerframework.dataflow
 * analysis. Contains a set of annotations.
 *
 * @author Stefan Heule
 *
 */
public abstract class CFAbstractValue<V extends CFAbstractValue<V>> implements
        AbstractValue<V> {

    /**
     * The analysis class this store belongs to.
     */
    protected final CFAbstractAnalysis<V, ?, ?> analysis;

    protected final TypeHierarchy typeHierarchy;

    /**
     * The type (with annotations) corresponding to this abstract value.
     */
    protected final AnnotatedTypeMirror type;

    public CFAbstractValue(CFAbstractAnalysis<V, ?, ?> analysis,
            AnnotatedTypeMirror type) {
        assert AnnotatedTypes.isValidType(analysis.qualifierHierarchy, type) : "Encountered invalid type: "
                + type.toString(true);
        this.analysis = analysis;
        this.type = type;
        this.typeHierarchy = analysis.getTypeHierarchy();
    }

    @Pure // because the type field is final
    public AnnotatedTypeMirror getType() {
        return type;
    }

    @Override
    public V leastUpperBound(/*@Nullable*/ V other) {
        if (other == null) {
            @SuppressWarnings("unchecked")
            V v = (V) this;
            return v;
        }
        AnnotatedTypeMirror otherType = other.getType();
        AnnotatedTypeMirror type = getType();

        GenericAnnotatedTypeFactory<V, ?, ?, ?> factory = analysis.getTypeFactory();
        ProcessingEnvironment processingEnv = factory.getProcessingEnv();
        AnnotatedTypeMirror lubAnnotatedType = AnnotatedTypes.leastUpperBound(processingEnv, factory,
                type, otherType);

        return analysis.createAbstractValue(lubAnnotatedType);
    }

    private static void copyArrayComponentAnnotations(AnnotatedArrayType source,
            AnnotatedArrayType dest) {
        AnnotatedTypeMirror destComp = dest.getComponentType();
        AnnotatedTypeMirror sourceComp = source.getComponentType();
        destComp.addAnnotations(sourceComp.getAnnotations());
        if (sourceComp instanceof AnnotatedArrayType) {
            assert dest instanceof AnnotatedArrayType;
            copyArrayComponentAnnotations((AnnotatedArrayType) sourceComp,
                    (AnnotatedArrayType) destComp);
        }
    }

    /**
     * Returns the annotations on the upper bound of type {@code t}.
     */  //TODO_JB: INTERSECTIONS AREN'T TAKEN CARE OF
    private static Collection<AnnotationMirror> getUpperBound(
            AnnotatedTypeMirror t) {
        if (t.getKind() == TypeKind.WILDCARD) {
            AnnotatedTypeMirror extendsBound = ((AnnotatedWildcardType) t)
                    .getExtendsBound();
            if (extendsBound != null) {
                return extendsBound.getEffectiveAnnotations();
            }
        } else if (t.getKind() == TypeKind.TYPEVAR) {
            AnnotatedTypeMirror upperBound = ((AnnotatedTypeVariable) t)
                    .getUpperBound();
            if (upperBound != null) {
                return upperBound.getEffectiveAnnotations();
            }
        }
        return t.getEffectiveAnnotations();
    }

    /**
     * Returns whether this value is a subtype of the argument {@code other}.
     * The annotations are compared per hierarchy, and missing annotations are
     * treated as 'top'.
     */
    public boolean isSubtypeOf(CFAbstractValue<V> other) {
        if (other == null) {
            // 'null' is 'top'
            return true;
        }
        return typeHierarchy.isSubtype(type, other.getType());
    }

    /**
     * Returns the more specific version of two values {@code this} and
     * {@code other}. If they do not contain information for all hierarchies,
     * then it is possible that information from both {@code this} and
     * {@code other} are taken.
     *
     * <p>
     * If neither of the two is more specific for one of the hierarchies (i.e.,
     * if the two are incomparable as determined by
     * {@link TypeHierarchy#isSubtype(AnnotatedTypeMirror, AnnotatedTypeMirror)}
     * , then the respective value from {@code backup} is used.
     *
     * <p>
     * TODO: The code in this method is rather similar to
     * {@link #leastUpperBound(CFAbstractValue)}. Can code be reused?
     */
    public V mostSpecific(/*@Nullable*/ V other, /*@Nullable*/ V backup) {
        if (other == null) {
            @SuppressWarnings("unchecked")
            V v = (V) this;
            return v;
        }
        // Create new full type (with the same underlying type), and then add
        // the appropriate annotations.
        TypeMirror underlyingType = InternalUtils.greatestLowerBound(analysis
                .getEnv(), getType().getUnderlyingType(), other.getType()
                .getUnderlyingType());

        underlyingType = handleTypeVarIntersections(
                getType().getUnderlyingType(), other.getType().getUnderlyingType(), underlyingType);

        if (underlyingType.getKind() == TypeKind.ERROR
                || underlyingType.getKind() == TypeKind.NONE) {
            // pick one of the option
            if (backup != null) {
                underlyingType = backup.getType().getUnderlyingType();
            } else {
                underlyingType = this.getType().getUnderlyingType();
            }
        }
        AnnotatedTypeMirror result = AnnotatedTypeMirror.createType(
                underlyingType, analysis.getTypeFactory(), false);
        AnnotatedTypeMirror otherType = other.getType();

        if (mostSpecific(analysis.getTypeFactory(), getType(), otherType,
                backup == null ? null : backup.getType(), result)) {
            return analysis.createAbstractValue(result);
        } else {
            return backup;
        }
    }

    /**
     * TODO: RETHINK THESE WHEN WE ACTUALLY CONSIDER THE INTENDED SEMANTICS OF INTERSECTIONS
     * When data flow encounters the following:
     * <pre>{@code
     * <T> void method(T t) {
     *     if (t instance of Cloneable) {
     *     }
     * }
     * }</pre>
     *
     * We will take glb(T, Cloneable) and it will yield T &amp; Cloneable
     * Much of our handling of intersection types relies on the fact that you cannot write a
     * type variable in an intersection bound.  This will cause many errors.
     *
     * Since t must be a type T, it is useful just to use T itself as the mostSpecific type,
     * so we will use it for now.
     */
    private static TypeMirror handleTypeVarIntersections(TypeMirror thisType, TypeMirror other, TypeMirror glbType) {

        TypeMirror typeVar = null;
        if (  thisType.getKind() == TypeKind.TYPEVAR ) {
            typeVar = thisType;
        } else if ( other.getKind() == TypeKind.TYPEVAR ) {
            typeVar = other;
        }

        if (typeVar != null && glbType.getKind() == TypeKind.INTERSECTION) {
            for (TypeMirror bound : ((IntersectionType) glbType).getBounds()) {
                if (bound.equals(typeVar)) {
                    return typeVar;
                }
            }
        }

        return glbType;
    }

    /**
     * Implementation of {@link #mostSpecific(CFAbstractValue, CFAbstractValue)}
     * that works on {@link AnnotatedTypeMirror}s.
     *
     * @return true iff result could be set to a uniquely most specific type
     */
    private static boolean mostSpecific(AnnotatedTypeFactory typeFactory,
                                        AnnotatedTypeMirror a, AnnotatedTypeMirror b,
                                        AnnotatedTypeMirror backup, AnnotatedTypeMirror result) {
        boolean canContainEmpty = QualifierHierarchy.canHaveEmptyAnnotationSet(a)
                && QualifierHierarchy.canHaveEmptyAnnotationSet(b)
                && QualifierHierarchy.canHaveEmptyAnnotationSet(result);

        final TypeKind resultKind = result.getKind();
        if (resultKind == TypeKind.TYPEVAR ) {
            return mostSpecificTypeVariable(typeFactory, a, b, backup, (AnnotatedTypeVariable) result);

        } else {
            final QualifierHierarchy qualHierarchy = typeFactory.getQualifierHierarchy();
            for (AnnotationMirror top : qualHierarchy.getTopAnnotations()) {
                AnnotationMirror aAnno =
                        canContainEmpty ? a.getAnnotationInHierarchy(top)
                                        : a.getEffectiveAnnotationInHierarchy(top);

                AnnotationMirror bAnno =
                        canContainEmpty ? b.getAnnotationInHierarchy(top)
                                        : b.getEffectiveAnnotationInHierarchy(top);

                if (qualHierarchy.isSubtype(a, b, aAnno, bAnno)) {
                    if (aAnno == null) {
                        result.removeAnnotationInHierarchy(top);
                    } else {
                        result.addAnnotation(aAnno);
                    }
                } else if (qualHierarchy.isSubtype(a, b, bAnno, aAnno)) {
                    if (bAnno == null) {
                        result.removeAnnotationInHierarchy(top);
                    } else {
                        result.addAnnotation(bAnno);
                    }
                } else {
                    if (backup != null) {
                        result.addAnnotation(backup.getAnnotationInHierarchy(top));
                    } else {
                        return false;
                    }
                }
            }

            if (resultKind == TypeKind.WILDCARD) {
                AnnotatedWildcardType wResult = (AnnotatedWildcardType) result;
                Collection<AnnotationMirror> extendsBound1 = getUpperBound(a);
                Collection<AnnotationMirror> extendsBound2 = getUpperBound(b);
                wResult.addAnnotations(mostSpecific(qualHierarchy, extendsBound1, extendsBound2));

            } else if (a.getKind() == TypeKind.ARRAY
                    && b.getKind() == TypeKind.ARRAY) {
                AnnotatedArrayType aLubAnnotatedType = (AnnotatedArrayType) result;
                // for arrays, we have:
                // ms(@A1 A @A2[],@B1 A @B2[]) = ms(@A1,@B1) A
                // ms(@A2,@B2) []
                AnnotatedArrayType aa = (AnnotatedArrayType) a;
                AnnotatedArrayType bb = (AnnotatedArrayType) b;

                return mostSpecific(typeFactory, aa.getComponentType(),
                        bb.getComponentType(), null,
                        aLubAnnotatedType.getComponentType());
            } else if (resultKind == TypeKind.ARRAY) {
                AnnotatedArrayType aLubAnnotatedType = (AnnotatedArrayType) result;
                // copy annotations from the input array (either a or b)
                if (a.getKind() == TypeKind.ARRAY) {
                    copyArrayComponentAnnotations((AnnotatedArrayType) a, aLubAnnotatedType);
                } else {
                    assert b.getKind() == TypeKind.ARRAY;
                    copyArrayComponentAnnotations((AnnotatedArrayType) b, aLubAnnotatedType);
                }
            }
        }

        return true;
    }

    /**
     * Returns the set of annotations that is most specific from 'a' and 'b'.
     */
    private static Set<AnnotationMirror> mostSpecific(
            QualifierHierarchy qualHierarchy, Collection<AnnotationMirror> a,
            Collection<AnnotationMirror> b) {
        Set<AnnotationMirror> result = AnnotationUtils.createAnnotationSet();
        for (AnnotationMirror top : qualHierarchy.getTopAnnotations()) {
            AnnotationMirror aAnno = qualHierarchy.findCorrespondingAnnotation(
                    top, a);
            AnnotationMirror bAnno = qualHierarchy.findCorrespondingAnnotation(
                    top, b);
            assert aAnno != null : "Did not find an annotation for '" + top
                    + "' in '" + a + "'.";
            assert bAnno != null : "Did not find an annotation for '" + top
                    + "' in '" + b + "'.";
            if (qualHierarchy.isSubtype(aAnno, bAnno)) {
                result.add(aAnno);
            } else if (qualHierarchy.isSubtype(bAnno, aAnno)) {
                result.add(bAnno);
            } else {
                assert false : "Neither of the two values is more specific: "
                        + a + ", " + b + ".";
            }
        }
        return result;
    }

    /**
     * Refines the result annotated type variable with the most specific of type1 and type2.
     * @return True if the result was annotated in all hierarchy, false if the result could not be
     *         annotated in one or more annotation hierarchies
     */
    public static boolean mostSpecificTypeVariable(final AnnotatedTypeFactory typeFactory,
                                                   final AnnotatedTypeMirror type1, final AnnotatedTypeMirror type2,
                                                   final AnnotatedTypeMirror backup,
                                                   final AnnotatedTypeVariable result) {
        final Types types = typeFactory.getProcessingEnv().getTypeUtils();
        final TypeHierarchy typeHierarchy = typeFactory.getTypeHierarchy();
        final QualifierHierarchy qualifierHierarchy = typeFactory.getQualifierHierarchy();

        final AnnotatedTypeVariable declaredType =
           (AnnotatedTypeVariable) typeFactory.getAnnotatedType(result.getUnderlyingType().asElement());
        AnnotatedTypeMerger.merge(declaredType, result);


        //see issue422: Intersections can happen when we have a
        // if ( T instanceof String && T instanceof Integer )
        AnnotatedTypeMirror fixedType1;
        if (type1.getKind() == TypeKind.INTERSECTION) {
            fixedType1  = result.deepCopy();
            fixedType1.addAnnotations(AnnotatedTypes.glbOfBounds((AnnotatedIntersectionType) type1, qualifierHierarchy));
        } else {
            fixedType1 = type1;
        }

        AnnotatedTypeMirror fixedType2;
        if (type2.getKind() == TypeKind.INTERSECTION) {
            fixedType2  = result.deepCopy();
            fixedType2.addAnnotations(AnnotatedTypes.glbOfBounds((AnnotatedIntersectionType) type2, qualifierHierarchy));
        } else {
            fixedType2 = type2;
        }


        boolean annotated = true;
        for (final AnnotationMirror top : qualifierHierarchy.getTopAnnotations()) {
            if (typeHierarchy.isSubtype(fixedType1, fixedType2, top)) {
                annotateTypeVarResult(qualifierHierarchy, types, result, fixedType1, top);

            } else if (typeHierarchy.isSubtype(fixedType2, fixedType1, top)) {
                annotateTypeVarResult(qualifierHierarchy, types, result, fixedType2, top);

            } else {
                if (backup != null) {
                    annotateTypeVarResult(qualifierHierarchy, types, result, backup, top);
                } else {
                    annotated = false;
                }
            }
        }

        return annotated;
    }

    /**
     * Annotates result in the hierarchy denoted by top using the type mostSpecific as
     * a template.  Note, if mostSpecific is a type variable, wildcard, or intersection
     * the annotation may come from the bounds of that type.
     */
    private static void annotateTypeVarResult(final QualifierHierarchy qualifierHierarchy,
                                              final Types types,
                                              final AnnotatedTypeVariable result,
                                              final AnnotatedTypeMirror mostSpecific,
                                              final AnnotationMirror top) {
        final AnnotatedTypeMirror source = findSourceAtm(types, qualifierHierarchy, result, mostSpecific, top);
        final AnnotationMirror sourcePrimaryAnno = source.getAnnotationInHierarchy(top);

        //Indicates that source is a non-primary-annotated type variable declared by the same type parameter of result
        //in this case, source is equivalent to the declared type of that type variable and we copy its bounds
        if (types.isSameType(source.getUnderlyingType(), result.getUnderlyingType()) && sourcePrimaryAnno == null) {
            final AnnotatedTypeVariable resultDecl = (AnnotatedTypeVariable) source;

            final AnnotationMirror declUpperBoundAnno = resultDecl.getUpperBound().getAnnotationInHierarchy(top);
            final AnnotationMirror declLowerBoundAnno = resultDecl.getLowerBound().getAnnotationInHierarchy(top);

            if (declUpperBoundAnno != null) {
                result.getUpperBound().addAnnotation(declUpperBoundAnno);
            }

            if (declLowerBoundAnno != null) {
                result.getLowerBound().addAnnotation(declLowerBoundAnno);
            }
        } else {
            result.replaceAnnotation(sourcePrimaryAnno);
        }
    }

    /**
     * The source atm, is the location of the annotated type mirror that defines the types "upper bound".
     * For types that are exact (i.e. NOT TYPEVAR, WILDCARD, and INTERSECTION) this annotation is the primary
     * annotation.  For TYPEVAR, and WILDCARD it is the first annotation encountered while traversing their
     * upper bounds to get the concrete type.  Recall, the concrete type might be a few layers deep
     * (e.g. ? extends Y extends @SourceAnno Object).  For INTERSECTION types, we choose the most specific
     * annotation on its bounds.
     * Finally, if a TYPEVAR is a use of the same type variable as result, this becomes the source since
     * we will want to annotate the result with the same primary annotation or, if the primary annotation
     * is not present, the same bounds.
     *
     * @return The annotated type mirror that contains the upper bound primary annotation of toSearch
     */
    private static AnnotatedTypeMirror findSourceAtm(final Types types,
                                                     final QualifierHierarchy qualifierHierarchy,
                                                     final AnnotatedTypeVariable result,
                                                     final AnnotatedTypeMirror toSearch,
                                                     final AnnotationMirror top) {
        AnnotatedTypeMirror source = toSearch;
        while (source.getAnnotationInHierarchy(top) == null &&
                !types.isSameType(result.getUnderlyingType(), source.getUnderlyingType())) {

            switch (source.getKind()) {
                case TYPEVAR:
                    source = ((AnnotatedTypeVariable) source).getUpperBound();
                    break;

                case WILDCARD:
                    source = ((AnnotatedWildcardType) source).getExtendsBound();
                    break;

                case INTERSECTION:
                    source = mostSpecificSupertype(qualifierHierarchy, (AnnotatedIntersectionType) source, top);

                    if (source == null) {
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

    /**
     * For the given intersection type in the hierarchy of top, find the direct supertype that has the
     * most specific annotation.
     */
    private static AnnotatedTypeMirror mostSpecificSupertype(final QualifierHierarchy qualifierHierarchy,
                                                             final AnnotatedIntersectionType isect,
                                                             final AnnotationMirror top) {
        AnnotatedTypeMirror result = null;
        AnnotationMirror anno = null;
        for (final AnnotatedTypeMirror supertype : isect.directSuperTypes()) {
            final AnnotationMirror superAnno = supertype.getAnnotationInHierarchy(top);
            if (superAnno != null && (anno == null || qualifierHierarchy.isSubtype(superAnno, anno))) {
                anno = superAnno;
                result = supertype;
            }
        }

        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof CFAbstractValue)) {
            return false;
        }
        @SuppressWarnings("rawtypes")
        boolean result = getType().equals(((CFAbstractValue) obj).getType());
        return result;
    }

    @Pure
    @Override
    public int hashCode() {
        return HashCodeUtils.hash(type);
    }

    /**
     * @return The string representation as a comma-separated list.
     */
    @SideEffectFree
    @Override
    public String toString() {
        return getType().toString();
    }

}
