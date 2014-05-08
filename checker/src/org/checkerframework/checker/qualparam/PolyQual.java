package org.checkerframework.checker.qualparam;

public abstract class PolyQual<Q> {
    public abstract Q getMinimum();
    public abstract Q getMaximum();
    public abstract PolyQual<Q> substitute(String name, PolyQual<Q> value);

    public static final class GroundQual<Q> extends PolyQual<Q> {
        private final Q qual;

        public GroundQual(Q qual) {
            if (qual == null) {
                throw new IllegalArgumentException("qual must not be null");
            }
            this.qual = qual;
        }

        public Q getQualifier() {
            return qual;
        }

        @Override
        public Q getMinimum() {
            return qual;
        }

        @Override
        public Q getMaximum() {
            return qual;
        }

        @Override
        public PolyQual<Q> substitute(String name, PolyQual<Q> value) {
            return this;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || o.getClass() != this.getClass()) {
                return false;
            }
            @SuppressWarnings("rawtypes")
            GroundQual other = (GroundQual)o;
            return this.qual.equals(other.qual);
        }

        @Override
        public int hashCode() {
            return this.qual.hashCode();
        }

        @Override
        public String toString() {
            return qual.toString();
        }
    }

    public static final class QualVar<Q> extends PolyQual<Q> {
        private final String name;
        private final Q lower;
        private final Q upper;

        public QualVar(String name, Q lower, Q upper) {
            if (name == null) {
                throw new IllegalArgumentException("name must not be null");
            }
            if (lower == null || upper == null) {
                throw new IllegalArgumentException("bounds must not be null");
            }
            this.name = name;
            this.lower = lower;
            this.upper = upper;
        }

        public String getName() {
            return name;
        }

        public Q getLowerBound() {
            return lower;
        }

        public Q getUpperBound() {
            return upper;
        }

        @Override
        public Q getMinimum() {
            return lower;
        }

        @Override
        public Q getMaximum() {
            return upper;
        }

        @Override
        public PolyQual<Q> substitute(String name, PolyQual<Q> value) {
            if (name.equals(this.name)) {
                return value;
            } else {
                return this;
            }
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || o.getClass() != this.getClass()) {
                return false;
            }
            @SuppressWarnings("rawtypes")
            QualVar other = (QualVar)o;
            return this.name.equals(other.name)
                && this.lower.equals(other.lower)
                && this.upper.equals(other.upper);
        }

        @Override
        public int hashCode() {
            return this.name.hashCode() * 13
                + this.lower.hashCode() * 37
                + this.upper.hashCode() * 59;
        }

        @Override
        public String toString() {
            return "(" + name + " ∈ [" + lower + ".." + upper + "])";
        }
    }

    // TODO
    /*
    public static class Combined<Q> extends PolyQual<Q> {
    }
    */
}
