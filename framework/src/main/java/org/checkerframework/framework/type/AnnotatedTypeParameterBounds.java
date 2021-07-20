package org.checkerframework.framework.type;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Objects;

/** Represents upper and lower bounds, each an AnnotatedTypeMirror. */
public class AnnotatedTypeParameterBounds {
    private final AnnotatedTypeMirror upper;
    private final AnnotatedTypeMirror lower;

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

    @Override
    public String toString() {
        return "[extends " + upper + " super " + lower + "]";
    }

    /**
     * Return a possibly-verbose string representation of this.
     *
     * @param verbose if true, returned representation is verbose
     * @return a possibly-verbose string representation of this
     */
    public String toString(boolean verbose) {
        return "[extends " + upper.toString(verbose) + " super " + lower.toString(verbose) + "]";
    }

    @Override
    public int hashCode() {
        return Objects.hash(upper, lower);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (!(obj instanceof AnnotatedTypeParameterBounds)) {
            return false;
        }
        AnnotatedTypeParameterBounds other = (AnnotatedTypeParameterBounds) obj;
        return this.upper == null
                ? other.upper == null
                : this.upper.equals(other.upper) && this.lower == null
                        ? other.lower == null
                        : this.lower.equals(other.lower);
    }
}
