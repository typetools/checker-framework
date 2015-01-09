package org.checkerframework.qualframework.poly;

import java.util.*;

/** A qualifier in the qualifier polymorphism system, with ground qualifier
 * representation {@code Q}.  A {@code PolyQual<Q>} represents either a
 * qualifier from the original system ({@code GroundQual}, which simply wraps a
 * {@code Q}), a qualifier variable that ranges over ground qualifiers ({@code
 * QualVar}), or a combination of two or more qualifiers using a {@link
 * CombiningOperation} ({@code Combined}).
 */
public abstract class PolyQual<Q> {
    /** Get the greatest lower bound of the possible types of this qualifier
     * under all valid assignments to qualifier variables.  (That is, for all
     * assignments of qualifiers to qualifier variables, {@code
     * pq.getMinimum()} is a subtype of {@code pq}.
     */
    public abstract Q getMinimum();

    /** Get the least upper bound of the possible types of this qualifier under
     * all valid assignments to qualifier variables.
     */
    public abstract Q getMaximum();

    /** Substitute qualifiers for qualifier variables.
     */
    public abstract PolyQual<Q> substitute(Map<String, PolyQual<Q>> substs);

    /** Convert this qualifier to an equivalent {@link Combined} qualifier
     * using the given {@link CombiningOperation}.
     */
    public abstract Combined<Q> asCombined(CombiningOperation<Q> op);

    /** Combine this qualifier with another using the given {@link
     * CombiningOperation}.
     */
    public PolyQual<Q> combineWith(PolyQual<Q> other, CombiningOperation<Q> op) {
        return this.asCombined(op).combineWith(other.asCombined(op));
    }

    /** A wrapped qualifier from the underlying system. */
    public static final class GroundQual<Q> extends PolyQual<Q> {
        private final Q qual;

        public GroundQual(Q qual) {
            if (qual == null) {
                throw new IllegalArgumentException("qual must not be null");
            }
            this.qual = qual;
        }

        /** Get the underlying qualifier representation. */
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
        public PolyQual<Q> substitute(Map<String, PolyQual<Q>> substs) {
            return this;
        }

        @Override
        public Combined<Q> asCombined(CombiningOperation<Q> op) {
            return new Combined<Q>(op, qual);
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

    /** A qualifier variable. */
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

        /** Get the name of this qualifier variable. */
        public String getName() {
            return name;
        }

        /** Get the lower bound of this qualifier variable. */
        public Q getLowerBound() {
            return lower;
        }

        /** Get the upper bound of this qualifier variable. */
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
        public PolyQual<Q> substitute(Map<String, PolyQual<Q>> substs) {
            PolyQual<Q> value = substs.get(this.name);
            if (value != null) {
                return value;
            } else {
                return this;
            }
        }

        @Override
        public Combined<Q> asCombined(CombiningOperation<Q> op) {
            return new Combined<Q>(op, this);
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

    /** A combination of several qualifiers and qualifier variables, using a
     * combining function. */
    public static class Combined<Q> extends PolyQual<Q> {
        // We keep only a single `Q` field because combining two Qs can be done
        // immediately using `op` - unlike combining `QualVar`s, which we can't
        // do much with until they have been substituted.
        private final CombiningOperation<Q> op;
        private final HashSet<QualVar<Q>> vars;
        private final Q ground;

        public Combined(CombiningOperation<Q> op, Collection<QualVar<Q>> vars, Q ground) {
            this.op = op;
            this.vars = new HashSet<>(vars);
            this.ground = ground;
        }

        public Combined(CombiningOperation<Q> op, QualVar<Q> var, Q ground) {
            this.op = op;
            this.vars = new HashSet<>();
            this.vars.add(var);
            this.ground = ground;
        }

        public Combined(CombiningOperation<Q> op, QualVar<Q> var) {
            this.op = op;
            this.vars = new HashSet<>();
            this.vars.add(var);
            this.ground = op.identity();
        }

        public Combined(CombiningOperation<Q> op, Q ground) {
            this.op = op;
            this.vars = new HashSet<>();
            this.ground = ground;
        }

        /** Like the main {@code Combined<Q>} constructor, but returns a simpler
         * PolyQual (GroundQual or QualVar) when possible.
         */
        public static <Q> PolyQual<Q> from(CombiningOperation<Q> op, Collection<QualVar<Q>> vars, Q ground) {
            if (vars.isEmpty()) {
                return new GroundQual<Q>(ground);
            }

            if (vars.size() == 1 && ground.equals(op.identity())) {
                for (QualVar<Q> var : vars) {
                    return var;
                }
            }

            return new Combined<Q>(op, vars, ground);
        }

        /** Combine two instances of {@code Combined} that were built with the
         * same {@link CombiningOperation}.
         */
        public PolyQual<Q> combineWith(Combined<Q> other) {
            if (this.op != other.op) {
                throw new IllegalArgumentException(
                        "can't combine two Combined<Q> using different CombiningOperations");
            }
            HashSet<QualVar<Q>> newVars = new HashSet<>(this.vars);
            newVars.addAll(other.vars);
            Q newGround = this.op.combine(this.ground, other.ground);
            return Combined.from(op, newVars, newGround);
        }

        public CombiningOperation<Q> getOp() {
            return op;
        }

        public Set<QualVar<Q>> getVars() {
            return vars;
        }

        public Q getGround() {
            return ground;
        }

        @Override
        public Q getMinimum() {
            Q result = ground;
            for (QualVar<Q> var : vars) {
                result = op.combine(result, var.getMinimum());
            }
            return result;
        }

        @Override
        public Q getMaximum() {
            Q result = ground;
            for (QualVar<Q> var : vars) {
                result = op.combine(result, var.getMaximum());
            }
            return result;
        }

        @Override
        public PolyQual<Q> substitute(Map<String, PolyQual<Q>> substs) {
            HashSet<QualVar<Q>> newVars = new HashSet<>();
            Q newGround = ground;

            for (QualVar<Q> var : vars) {
                Combined<Q> substCombined = var.substitute(substs).asCombined(op);
                newVars.addAll(substCombined.vars);
                newGround = op.combine(newGround, substCombined.ground);
            }

            return Combined.from(op, newVars, newGround);
        }

        @Override
        public Combined<Q> asCombined(CombiningOperation<Q> op) {
            if (op != this.op) {
                // We might have already used `self.op` to combine several
                // GroundQuals.  Switching `op` after that could lead to crazy
                // results.
                throw new IllegalArgumentException(
                        "can't call Combined.asCombined with different CombiningOperation");
            }

            return this;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || o.getClass() != this.getClass()) {
                return false;
            }
            @SuppressWarnings("rawtypes")
            Combined other = (Combined)o;
            return this.op.equals(other.op)
                && this.vars.equals(other.vars)
                && this.ground.equals(other.ground);
        }

        @Override
        public int hashCode() {
            return this.op.hashCode() * 13
                + this.vars.hashCode() * 37
                + this.ground.hashCode() * 59;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(op);
            sb.append("(");
            for (QualVar<Q> var : vars) {
                sb.append(var);
                sb.append(", ");
            }
            sb.append(ground);
            sb.append(")");
            return sb.toString();
        }
    }
}
