package org.checkerframework.qualframework.poly;

import org.checkerframework.qualframework.base.QualifierHierarchy;

/** A binary operation for combining qualifiers of type {@code Q}.
 */
public interface CombiningOperation<Q> {
    /** Apply the operation to two qualifiers, producing a new qualifier.  The
     * operation is expected to be commutative and associative.
     */
    Q combine(Q a, Q b);

    /** The identity element for this operation.  It should always be the case
     * that {@code op.combine(op.identity(), x)} is equivalent to {@code x}.
     */
    Q identity();

    /** The least-upper-bound operation over a qualifier hierarchy.
     */
    public static class Lub<Q> implements CombiningOperation<Q> {
        QualifierHierarchy<Q> hierarchy;

        public Lub(QualifierHierarchy<Q> hierarchy) {
            this.hierarchy = hierarchy;
        }

        @Override
        public Q combine(Q a, Q b) {
            return hierarchy.leastUpperBound(a, b);
        }

        @Override
        public Q identity() {
            return hierarchy.getBottom();
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || o.getClass() != this.getClass()) {
                return false;
            }
            @SuppressWarnings("rawtypes")
            Lub other = (Lub)o;
            return this.hierarchy.equals(other.hierarchy);
        }

        @Override
        public int hashCode() {
            return this.hierarchy.hashCode();
        }

        @Override
        public String toString() {
            return "Lub";
        }
    }

    /** The greatest-lower-bound operation over a qualifier hierarchy.
     */
    public static class Glb<Q> implements CombiningOperation <Q> {
        QualifierHierarchy<Q> hierarchy;

        public Glb(QualifierHierarchy<Q> hierarchy) {
            this.hierarchy = hierarchy;
        }

        @Override
        public Q combine(Q a, Q b) {
            return hierarchy.greatestLowerBound(a, b);
        }

        @Override
        public Q identity() {
            return hierarchy.getTop();
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || o.getClass() != this.getClass()) {
                return false;
            }
            @SuppressWarnings("rawtypes")
            Glb other = (Glb)o;
            return this.hierarchy.equals(other.hierarchy);
        }

        @Override
        public int hashCode() {
            return this.hierarchy.hashCode();
        }

        @Override
        public String toString() {
            return "Glb";
        }
    }
}
