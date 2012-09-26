package checkers.flow.analysis.checkers;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeMirror;

import checkers.basetype.BaseTypeChecker;
import checkers.flow.analysis.AbstractValue;
import checkers.flow.util.HashCodeUtils;
import checkers.types.AbstractBasicAnnotatedTypeFactory;
import checkers.types.AnnotatedTypeMirror;
import checkers.types.QualifierHierarchy;
import checkers.types.TypeHierarchy;
import checkers.util.InternalUtils;

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
        QualifierHierarchy qualifierHierarchy = analysis.getFactory().getQualifierHierarchy();
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
        lubAnnotatedType.addAnnotations(qualifierHierarchy
                .leastUpperBounds(getType(), other.getType(), getType().getAnnotations(),
                        other.getType().getAnnotations()));
        return analysis.createAbstractValue(lubAnnotatedType);
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
     */
    public V mostSpecific(/* @Nullable */V other, /* @Nullable */V backup) {
        if (other == null) {
            @SuppressWarnings("unchecked")
            V v = (V) this;
            return v;
        }
        // Create new full type (with the same underlying type), and then add
        // the appropriate annotations.
        AnnotatedTypeMirror result = AnnotatedTypeMirror.createType(
                type.getUnderlyingType(), analysis.getFactory());
        QualifierHierarchy qualHierarchy = analysis.getFactory()
                .getQualifierHierarchy();
        AnnotatedTypeMirror otherType = other.getType();
        for (AnnotationMirror top : qualHierarchy.getTopAnnotations()) {
            AnnotationMirror aAnno = getType().getAnnotationInHierarchy(top);
            AnnotationMirror bAnno = otherType.getAnnotationInHierarchy(top);

            if (qualHierarchy.isSubtype(getType(), otherType, aAnno, bAnno)) {
                if (aAnno == null) {
                    result.removeAnnotationInHierarchy(top);
                } else {
                    result.addAnnotation(aAnno);
                }
            } else if (qualHierarchy.isSubtype(getType(), otherType, bAnno, aAnno)) {
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
        return analysis.createAbstractValue(result);
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