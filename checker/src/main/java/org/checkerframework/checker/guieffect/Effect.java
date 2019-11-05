package org.checkerframework.checker.guieffect;

import java.lang.annotation.Annotation;
import org.checkerframework.checker.guieffect.qual.PolyUIEffect;
import org.checkerframework.checker.guieffect.qual.SafeEffect;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;

public final class Effect {
    // Colin hates Java's comparable interface, so he's not using it

    private final Class<? extends Annotation> annotClass;

    public Effect(Class<? extends Annotation> cls) {
        assert cls == UIEffect.class || cls == PolyUIEffect.class || cls == SafeEffect.class;
        annotClass = cls;
    }

    public static boolean lessThanOrEqualTo(Effect left, Effect right) {
        assert (left != null && right != null);
        boolean leftBottom = left.annotClass == SafeEffect.class;
        boolean rightTop = right.annotClass == UIEffect.class;
        return leftBottom || rightTop || left.annotClass == right.annotClass;
    }

    public static Effect min(Effect l, Effect r) {
        if (lessThanOrEqualTo(l, r)) {
            return l;
        } else {
            return r;
        }
    }

    public static final class EffectRange {
        public final Effect min, max;

        public EffectRange(Effect min, Effect max) {
            assert (min != null || max != null);
            // If one is null, fill in with the other
            this.min = (min != null ? min : max);
            this.max = (max != null ? max : min);
        }
    }

    public boolean isSafe() {
        return annotClass == SafeEffect.class;
    }

    public boolean isUI() {
        return annotClass == UIEffect.class;
    }

    public boolean isPoly() {
        return annotClass == PolyUIEffect.class;
    }

    public Class<? extends Annotation> getAnnot() {
        return annotClass;
    }

    @SideEffectFree
    @Override
    public String toString() {
        return annotClass.getSimpleName();
    }

    @SuppressWarnings("NonOverridingEquals") // TODO: clean this up!
    public boolean equals(Effect e) {
        return annotClass == e.annotClass;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Effect) {
            return this.equals((Effect) o);
        } else {
            return super.equals(o);
        }
    }

    @Pure
    @Override
    public int hashCode() {
        return 31 + annotClass.hashCode();
    }
}
