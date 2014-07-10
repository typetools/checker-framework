package org.checkerframework.framework.type;

public class AnnotatedTypeParameterBounds {
    private AnnotatedTypeMirror upper;
    private AnnotatedTypeMirror lower;

    public AnnotatedTypeParameterBounds(AnnotatedTypeMirror upper, AnnotatedTypeMirror lower) {
        this.upper = upper;
        this.lower = lower;
    }

    public AnnotatedTypeMirror getUpperBound() {
        return upper;
    }

    public AnnotatedTypeMirror getLowerBound() {
        return lower;
    }

    public String toString() {
        return "[extends " + upper + " super " + lower + "]";
    }

    public int hashCode() {
        return 17 * upper.hashCode()
            + 37 * lower.hashCode();
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof AnnotatedTypeParameterBounds)) {
            return false;
        }
        AnnotatedTypeParameterBounds other = (AnnotatedTypeParameterBounds)obj;
        return this.upper == null ? other.upper == null : this.upper.equals(other.upper)
            && this.lower == null ? other.lower == null : this.lower.equals(other.lower);
    }
}
