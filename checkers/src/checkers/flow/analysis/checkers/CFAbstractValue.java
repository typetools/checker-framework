package checkers.flow.analysis.checkers;

import java.util.Collection;
import java.util.Set;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

import javacutils.AnnotationUtils;
import javacutils.InternalUtils;

import dataflow.analysis.AbstractValue;
import dataflow.util.HashCodeUtils;

import checkers.basetype.BaseTypeChecker;
import checkers.types.AbstractBasicAnnotatedTypeFactory;
import checkers.types.AnnotatedTypeMirror;
import checkers.types.AnnotatedTypeMirror.AnnotatedTypeVariable;
import checkers.types.AnnotatedTypeMirror.AnnotatedWildcardType;
import checkers.types.QualifierHierarchy;
import checkers.types.TypeHierarchy;

/**
 * An implementation of an abstract value used by the Checker Framework dataflow
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
        this.analysis = analysis;
        this.type = type;
        this.typeHierarchy = analysis.getTypeHierarchy();
        assert type != null;
        QualifierHierarchy qualifierHierarchy = analysis.getFactory()
                .getQualifierHierarchy();
        int width = qualifierHierarchy.getWidth();
        if (!QualifierHierarchy.canHaveEmptyAnnotationSet(type)) {
            assert width == type.getAnnotations().size() : "Encountered type with an invalid number of annotations ("
                    + type.getAnnotations().size()
                    + ", should be "
                    + width
                    + "): " + type;
        }
    }

    public AnnotatedTypeMirror getType() {
        return type;
    }

    /**
     * Computes and returns the least upper bound of two sets of type
     * annotations.
     *
     * <p>
     * TODO: The code in this method is rather similar to
     * {@link #mostSpecific(CFAbstractValue, CFAbstractValue)}. Can code be
     * reused?
     */
    @Override
    public V leastUpperBound(/* @Nullable */V other) {
        if (other == null) {
            @SuppressWarnings("unchecked")
            V v = (V) this;
            return v;
        }
        AbstractBasicAnnotatedTypeFactory<? extends BaseTypeChecker, V, ?, ?, ?> factory = analysis
                .getFactory();
        ProcessingEnvironment processingEnv = factory.getProcessingEnv();
        TypeMirror lubType = InternalUtils.leastUpperBound(processingEnv,
                getType().getUnderlyingType(), other.getType()
                        .getUnderlyingType());
        AnnotatedTypeMirror lubAnnotatedType = AnnotatedTypeMirror.createType(
                lubType, factory);
        QualifierHierarchy qualifierHierarchy = analysis.qualifierHierarchy;

        Set<AnnotationMirror> annos1;
        Set<AnnotationMirror> annos2;
        if (QualifierHierarchy.canHaveEmptyAnnotationSet(lubAnnotatedType)) {
            annos1 = getType().getAnnotations();
            annos2 = other.getType().getAnnotations();
        } else {
            annos1 = getType().getEffectiveAnnotations();
            annos2 = other.getType().getEffectiveAnnotations();
        }

        lubAnnotatedType.addAnnotations(qualifierHierarchy.leastUpperBounds(
                getType(), other.getType(), annos1, annos2));

        if (lubAnnotatedType.getKind() == TypeKind.WILDCARD) {
            AnnotatedWildcardType wLubAnnotatedType = (AnnotatedWildcardType) lubAnnotatedType;
            AnnotatedTypeMirror extendsBound = wLubAnnotatedType
                    .getExtendsBound();
            extendsBound.clearAnnotations();
            Collection<AnnotationMirror> extendsBound1 = getUpperBound(getType());
            Collection<AnnotationMirror> extendsBound2 = getUpperBound(other
                    .getType());
            extendsBound.addAnnotations(qualifierHierarchy.leastUpperBounds(
                    extendsBound1, extendsBound2));
        } else if (lubAnnotatedType.getKind() == TypeKind.TYPEVAR) {
            AnnotatedTypeVariable tLubAnnotatedType = (AnnotatedTypeVariable) lubAnnotatedType;
            AnnotatedTypeMirror upperBound = tLubAnnotatedType.getUpperBound();
            Collection<AnnotationMirror> upperBound1 = getUpperBound(getType());
            Collection<AnnotationMirror> upperBound2 = getUpperBound(other
                    .getType());

            // TODO: how is it possible that uppBound1 or 2 does not have any
            // annotations?
            if (upperBound1.size() != 0 && upperBound2.size() != 0) {
                upperBound.clearAnnotations();
                upperBound.addAnnotations(qualifierHierarchy.leastUpperBounds(
                        upperBound1, upperBound2));
            }
        }

        return analysis.createAbstractValue(lubAnnotatedType);
    }

    /**
     * Returns the annotations on the upper bound of type {@code t}.
     */
    private Collection<AnnotationMirror> getUpperBound(AnnotatedTypeMirror t) {
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
     * {@link #isSubtype(int, InferredAnnotation, InferredAnnotation)}, then the
     * respective value from {@code backup} is used. If {@code backup} is
     * {@code null}, then an assertion error is raised.
     *
     * <p>
     * TODO: The code in this method is rather similar to
     * {@link #leastUpperBound(CFAbstractValue)}. Can code be reused?
     */
    public V mostSpecific(/* @Nullable */V other, /* @Nullable */V backup) {
        if (other == null) {
            @SuppressWarnings("unchecked")
            V v = (V) this;
            return v;
        }
        // Create new full type (with the same underlying type), and then add
        // the appropriate annotations.
        TypeMirror underlyingType = InternalUtils.leastUpperBound(analysis
                .getEnv(), getType().getUnderlyingType(), other.getType()
                .getUnderlyingType());
        AnnotatedTypeMirror result = AnnotatedTypeMirror.createType(
                underlyingType, analysis.getFactory());
        QualifierHierarchy qualHierarchy = analysis.getFactory()
                .getQualifierHierarchy();
        AnnotatedTypeMirror otherType = other.getType();

        boolean canContainEmpty = underlyingType.getKind() == TypeKind.TYPEVAR
                || underlyingType.getKind() == TypeKind.WILDCARD;

        for (AnnotationMirror top : qualHierarchy.getTopAnnotations()) {
            AnnotationMirror aAnno = canContainEmpty ? getType()
                    .getAnnotationInHierarchy(top) : getType()
                    .getEffectiveAnnotationInHierarchy(top);
            AnnotationMirror bAnno = canContainEmpty ? otherType
                    .getAnnotationInHierarchy(top) : otherType
                    .getEffectiveAnnotationInHierarchy(top);

            if (qualHierarchy.isSubtype(getType(), otherType, aAnno, bAnno)) {
                if (aAnno == null) {
                    result.removeAnnotationInHierarchy(top);
                } else {
                    result.addAnnotation(aAnno);
                }
            } else if (qualHierarchy.isSubtype(getType(), otherType, bAnno,
                    aAnno)) {
                if (bAnno == null) {
                    result.removeAnnotationInHierarchy(top);
                } else {
                    result.addAnnotation(bAnno);
                }
            } else {
                if (backup == null) {
                    assert false : "Neither of the two values is more specific: "
                            + this + ", " + other + ".";
                } else {
                    result.addAnnotation(backup.getType()
                            .getAnnotationInHierarchy(top));
                }
            }
        }

        if (result.getKind() == TypeKind.WILDCARD) {
            AnnotatedWildcardType wResult = (AnnotatedWildcardType) result;
            AnnotatedTypeMirror extendsBound = wResult.getExtendsBound();
            extendsBound.clearAnnotations();
            Collection<AnnotationMirror> extendsBound1 = getUpperBound(getType());
            Collection<AnnotationMirror> extendsBound2 = getUpperBound(other
                    .getType());
            extendsBound.addAnnotations(mostSpecific(extendsBound1,
                    extendsBound2));
        } else if (result.getKind() == TypeKind.TYPEVAR) {
            AnnotatedTypeVariable tResult = (AnnotatedTypeVariable) result;
            AnnotatedTypeMirror upperBound = tResult.getUpperBound();
            Collection<AnnotationMirror> upperBound1 = getUpperBound(getType());
            Collection<AnnotationMirror> upperBound2 = getUpperBound(other
                    .getType());

            // TODO: how is it possible that uppBound1 or 2 does not have any
            // annotations?
            if (upperBound1.size() != 0 && upperBound2.size() != 0) {
                upperBound.clearAnnotations();
                upperBound
                        .addAnnotations(mostSpecific(upperBound1, upperBound2));
            }
        }

        return analysis.createAbstractValue(result);
    }

    /**
     * Returns the set of annotations that is most specific from 'a' and 'b'.
     */
    private Iterable<? extends AnnotationMirror> mostSpecific(
            Collection<AnnotationMirror> a, Collection<AnnotationMirror> b) {
        Collection<AnnotationMirror> result = AnnotationUtils
                .createAnnotationSet();
        QualifierHierarchy qualHierarchy = analysis.qualifierHierarchy;
        for (AnnotationMirror top : qualHierarchy.getTopAnnotations()) {
            AnnotationMirror aAnno = qualHierarchy.findCorrespondingAnnotation(
                    top, a);
            AnnotationMirror bAnno = qualHierarchy.findCorrespondingAnnotation(
                    top, b);
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

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof CFAbstractValue)) {
            return false;
        }
        @SuppressWarnings("rawtypes")
        boolean result = getType().equals(((CFAbstractValue) obj).getType());
        return result;
    }

    @Override
    public int hashCode() {
        return HashCodeUtils.hash(type);
    }

    /**
     * @return The string representation as a comma-separated list.
     */
    @Override
    public String toString() {
        return getType().toString();
    }

    /**
     * Returns a string representation of an {@link AnnotationMirror}.
     */
    protected static String annotationToString(AnnotationMirror a) {
        String fullString = a.toString();
        int indexOfParen = fullString.indexOf("(");
        String annoName = fullString;
        if (indexOfParen >= 0) {
            annoName = fullString.substring(0, indexOfParen);
        }
        return fullString.substring(annoName.lastIndexOf('.') + 1,
                fullString.length());
    }
}
