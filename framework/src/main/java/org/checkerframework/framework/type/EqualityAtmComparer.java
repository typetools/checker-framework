package org.checkerframework.framework.type;

import org.checkerframework.checker.interning.qual.EqualsMethod;
import org.checkerframework.framework.type.visitor.EquivalentAtmComboScanner;
import org.checkerframework.javacutil.AnnotationUtils;
import org.plumelib.util.StringsPlume;

/**
 * Compares two annotated type mirrors for structural equality using only the primary annotations
 * and underlying types of the two input types and their component types. Note, this leaves out
 * other fields specific to some AnnotatedTypeMirrors (like directSupertypes, isUnderlyingTypeRaw,
 * isUninferredTypeArgument etc...). Ideally, both EqualityAtmComparer and HashcodeAtmVisitor would
 * visit relevant fields.
 *
 * <p>This class is used by AnnotatedTypeMirror#equals
 *
 * <p>This class should be kept synchronized with HashcodeAtmVisitor.
 *
 * @see org.checkerframework.framework.type.HashcodeAtmVisitor
 *     <p>Unlike HashcodeAtmVisitor this class is intended to be overridden.
 */
public class EqualityAtmComparer extends EquivalentAtmComboScanner<Boolean, Void> {

    /**
     * Called when a visit method is called on two types that do not have the same class, i.e. when
     * !type1.getClass().equals(type2.getClass())
     */
    @Override
    protected String defaultErrorMessage(
            AnnotatedTypeMirror type1, AnnotatedTypeMirror type2, Void v) {
        throw new UnsupportedOperationException(
                StringsPlume.joinLines(
                        "Comparing two different subclasses of AnnotatedTypeMirror.",
                        "type1=" + type1,
                        "type2=" + type2));
    }

    /** Return true if type1 and type2 have equivalent sets of annotations. */
    protected boolean arePrimeAnnosEqual(
            final AnnotatedTypeMirror type1, final AnnotatedTypeMirror type2) {
        return AnnotationUtils.areSame(type1.getAnnotations(), type2.getAnnotations());
    }

    /**
     * Return true if the twe types are the same.
     *
     * @param type1 the first type to compare
     * @param type2 the second type to compare
     * @return true if the twe types are the same
     */
    @EqualsMethod // to make Interning Checker permit the == comparison
    protected boolean compare(final AnnotatedTypeMirror type1, AnnotatedTypeMirror type2) {
        if (type1 == type2) {
            return true;
        }

        if (type1 == null || type2 == null) {
            return false;
        }

        @SuppressWarnings("TypeEquals") // TODO
        boolean sameUnderlyingType = type1.getUnderlyingType().equals(type2.getUnderlyingType());
        return sameUnderlyingType && arePrimeAnnosEqual(type1, type2);
    }

    @SuppressWarnings("interning:not.interned")
    @Override
    protected Boolean scanWithNull(
            AnnotatedTypeMirror type1, AnnotatedTypeMirror type2, Void aVoid) {
        // one of them should be null, therefore they are only equal if the other is null
        return type1 == type2;
    }

    @Override
    protected Boolean scan(AnnotatedTypeMirror type1, AnnotatedTypeMirror type2, Void v) {
        return compare(type1, type2) && reduce(true, super.scan(type1, type2, v));
    }

    /** Used to combine the results from component types or a type and its component types. */
    @Override
    protected Boolean reduce(Boolean r1, Boolean r2) {
        if (r1 == null) {
            return r2;
        } else if (r2 == null) {
            return r1;
        } else {
            return r1 && r2;
        }
    }
}
