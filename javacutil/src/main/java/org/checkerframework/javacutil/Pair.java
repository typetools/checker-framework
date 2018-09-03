package org.checkerframework.javacutil;

import java.util.Objects;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;

/**
 * Simple pair class for multiple returns.
 *
 * <p>TODO: as class is immutable, use @Covariant annotation.
 */
public class Pair<V1, V2> {
    public final V1 first;
    public final V2 second;

    private Pair(V1 v1, V2 v2) {
        this.first = v1;
        this.second = v2;
    }

    public static <V1, V2> Pair<V1, V2> of(V1 v1, V2 v2) {
        return new Pair<>(v1, v2);
    }

    @SideEffectFree
    @Override
    public String toString() {
        return "Pair(" + first + ", " + second + ")";
    }

    private int hashCode = -1;

    @Pure
    @Override
    public int hashCode() {
        if (hashCode == -1) {
            hashCode = Objects.hash(first, second);
        }
        return hashCode;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Pair)) {
            return false;
        }
        @SuppressWarnings("unchecked")
        Pair<V1, V2> other = (Pair<V1, V2>) o;
        return Objects.equals(this.first, other.first) && Objects.equals(this.second, other.second);
    }
}
