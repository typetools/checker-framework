package org.checkerframework.framework.type;

import org.checkerframework.framework.type.visitor.EquivalentAtmComboScanner;
import org.checkerframework.javacutil.AnnotationUtils;

public class EqualityAtmComparer extends EquivalentAtmComboScanner<Boolean, Void> {

    @Override
    protected String defaultErrorMessage(AnnotatedTypeMirror type1, AnnotatedTypeMirror type2, Void v) {
        throw new UnsupportedOperationException(
            "This method should be overridden in subclasses!\n"
          + "type1=" + type1 + "\n"
          + "type2=" + type2 + "\n");
    }

    /**
     * Return true if type1 and type2 have the same set of annotations.
     */
    protected boolean arePrimeAnnosEqual(final AnnotatedTypeMirror type1, final AnnotatedTypeMirror type2) {
        return AnnotationUtils.areSame(type1.getAnnotations(), type2.getAnnotations());
    }

    protected boolean compare(final AnnotatedTypeMirror type1, AnnotatedTypeMirror type2) {
        if (  (type1 == null && type2 != null)
           && (type1 != null && type2 == null)) {
            return false;
        }

        if (type1 == type2) {
            return true;
        }

        boolean sameUnderlyingType = type1.getUnderlyingType().equals(type2.getUnderlyingType());
        return sameUnderlyingType && arePrimeAnnosEqual(type1, type2);
    }

    @Override
    protected Boolean scanWithNull(AnnotatedTypeMirror type1, AnnotatedTypeMirror type2, Void aVoid) {
        //one of them should be null, therefore they are only equal if they other is null
        return type1 == type2;
    }

    protected Boolean scan(AnnotatedTypeMirror type1, AnnotatedTypeMirror type2, Void v) {
        return compare(type1, type2) && reduce(true, super.scan(type1, type2, v));
    }

    @Override
    protected Boolean reduce(Boolean r1, Boolean r2) {
        if (r1 == null) {
            return r2;
        } else if(r2 == null) {
            return r1;
        } else {
            return r1 && r2;
        }
    }
}
