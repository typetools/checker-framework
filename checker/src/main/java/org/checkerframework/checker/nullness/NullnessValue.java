package org.checkerframework.checker.nullness;

import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.PolyNull;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFAbstractValue;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.TypesUtils;

/**
 * Behaves just like {@link CFValue}, but additionally tracks whether at this point {@link PolyNull}
 * is known to be {@link NonNull} or {@link Nullable} (or not known to be either)
 */
public class NullnessValue extends CFAbstractValue<NullnessValue> {

    /** True if, at this point, {@link PolyNull} is known to be {@link NonNull}. */
    protected boolean isPolyNullNonNull;

    /** True if, at this point, {@link PolyNull} is known to be {@link Nullable}. */
    protected boolean isPolyNullNull;

    public NullnessValue(
            CFAbstractAnalysis<NullnessValue, ?, ?> analysis,
            Set<AnnotationMirror> annotations,
            TypeMirror underlyingType) {
        super(analysis, annotations, underlyingType);
    }

    @Override
    public NullnessValue leastUpperBound(NullnessValue other) {
        NullnessValue result = super.leastUpperBound(other);

        AnnotationMirror resultNullableAnno =
                analysis.getTypeFactory().getAnnotationByClass(result.annotations, Nullable.class);

        if (resultNullableAnno != null && other != null) {
            if ((this.isPolyNullNonNull
                            && this.containsNonNullOrPolyNull()
                            && other.isPolyNullNull
                            && other.containsNullableOrPolyNull())
                    || (other.isPolyNullNonNull
                            && other.containsNonNullOrPolyNull()
                            && this.isPolyNullNull
                            && this.containsNullableOrPolyNull())) {
                result.annotations.remove(resultNullableAnno);
                result.annotations.add(
                        ((NullnessAnnotatedTypeFactory) analysis.getTypeFactory()).POLYNULL);
            }
        }
        return result;
    }

    /**
     * Returns true if this value contans {@code @NonNull} or {@code @PolyNull}.
     *
     * @return true if this value contans {@code @NonNull} or {@code @PolyNull}
     */
    @Pure
    private boolean containsNonNullOrPolyNull() {
        return analysis.getTypeFactory().containsSameByClass(annotations, NonNull.class)
                || analysis.getTypeFactory().containsSameByClass(annotations, PolyNull.class);
    }

    /**
     * Returns true if this value contans {@code @Nullable} or {@code @PolyNull}.
     *
     * @return true if this value contans {@code @Nullable} or {@code @PolyNull}
     */
    @Pure
    private boolean containsNullableOrPolyNull() {
        return analysis.getTypeFactory().containsSameByClass(annotations, Nullable.class)
                || analysis.getTypeFactory().containsSameByClass(annotations, PolyNull.class);
    }

    @SideEffectFree
    @Override
    public String toStringSimple() {
        return "NV{"
                + AnnotationUtils.toStringSimple(annotations)
                + ", "
                + TypesUtils.simpleTypeName(underlyingType)
                + ", "
                + (isPolyNullNonNull ? 't' : 'f')
                + ' '
                + (isPolyNullNull ? 't' : 'f')
                + '}';
    }
}
