package org.checkerframework.checker.qualparam;

import org.checkerframework.qualframework.base.QualifierHierarchy;

public interface CombiningOperation<Q> {
    Q combine(Q a, Q b);
    Q identity();

    public static class HierarchyOperation<Q> implements CombiningOperation<Q> {
        private final QualifierHierarchy<Q> hierarchy;
        private final boolean useGlb;

        public HierarchyOperation(QualifierHierarchy<Q> hierarchy, boolean useGlb) {
            this.hierarchy = hierarchy;
            this.useGlb = useGlb;
        }

        public Q combine(Q a, Q b) {
            if (useGlb) {
                return hierarchy.greatestLowerBound(a, b);
            } else {
                return hierarchy.leastUpperBound(a, b);
            }
        }

        public Q identity() {
            if (useGlb) {
                return hierarchy.getTop();
            } else {
                return hierarchy.getBottom();
            }
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || o.getClass() != this.getClass()) {
                return false;
            }
            @SuppressWarnings("rawtypes")
            HierarchyOperation other = (HierarchyOperation)o;
            return this.hierarchy.equals(other.hierarchy)
                && this.useGlb == other.useGlb;
        }

        @Override
        public int hashCode() {
            return this.hierarchy.hashCode() * 17
                + (this.useGlb ? 37 : 0);
        }

        @Override
        public String toString() {
            return (useGlb ? "Glb" : "Lub");
        }
    }

    public static class Lub<Q> extends HierarchyOperation<Q> {
        public Lub(QualifierHierarchy<Q> hierarchy) {
            super(hierarchy, false);
        }
    }

    public static class Glb<Q> extends HierarchyOperation<Q> {
        public Glb(QualifierHierarchy<Q> hierarchy) {
            super(hierarchy, true);
        }
    }
}
