package org.checkerframework.checker.qualparam;

import org.checkerframework.qualframework.base.QualifierHierarchy;

/** A <code>BaseQual&lt;Q&gt;</code> wraps a qualifier of type <code>Q</code>
 * in a <code>ParamValue</code>.
 */
public class BaseQual<Q> extends ParamValue<Q> {
    private Q base;

    public BaseQual(Q base) {
        this.base = base;
    }

    public Q getBase(QualifierHierarchy<Q> hierarchy) {
        return base;
    }

    /** Returns a <code>BaseQual</code> object representing the top of the type
     * hierarchy.  This object is not equal to the result of <code>new
     * BaseQual(hierarchy.getTop())</code>, but it will produce an equivalent
     * <code>Q</code> when calling <code>getBase</code>.
     */
    public static <Q> BaseQual<Q> getTop() {
        return BaseLimit.<Q>getTop();
    }

    /** Returns a <code>BaseQual</code> object representing the bottom of the
     * type hierarchy.
     */
    public static <Q> BaseQual<Q> getBottom() {
        return BaseLimit.<Q>getBottom();
    }

    public ParamValue<Q> substitute(String name, ParamValue<Q> value) {
        return this;
    }

    public ParamValue<Q> capture() {
        return this;
    }

    public BaseQual<Q> getMinimum() {
        return this;
    }

    public BaseQual<Q> getMaximum() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !o.getClass().equals(BaseQual.class))
            return false;

        @SuppressWarnings("unchecked")
        BaseQual<Q> other = (BaseQual<Q>)o;
        return this.base.equals(other.base);
    }

    @Override
    public int hashCode() {
        return base.hashCode();
    }

    @Override
    public String toString() {
        return base.toString();
    }


    /** A class to represent the top or bottom of a type hierarchy.
     */
    private static class BaseLimit<Q> extends BaseQual<Q> {
        private final boolean isTop;

        private BaseLimit(boolean isTop) {
            super(null);
            this.isTop = isTop;
        }


        // The setup for getTop/getBottom is a bit ugly, but this is the only
        // way I've found to make it work.  Note that this is all perfectly
        // safe because BaseLimit never uses any values of type Q.
        //
        // TODO: See if we can make BaseLimit extend ParamValue<Q> instead of
        // BaseQual<Q>.  This makes it easier to see that BaseLimit and its
        // superclasses don't use any values of type Q.
        @SuppressWarnings("rawtypes")
        public static final BaseLimit BOTTOM = new BaseLimit(false);
        @SuppressWarnings("rawtypes")
        public static final BaseLimit TOP = new BaseLimit(true);

        @SuppressWarnings("unchecked")
        public static <Q> BaseLimit<Q> getTop() {
            return TOP;
        }

        @SuppressWarnings("unchecked")
        public static <Q> BaseLimit<Q> getBottom() {
            return BOTTOM;
        }


        @Override
        public Q getBase(QualifierHierarchy<Q> hierarchy) {
            return isTop ? hierarchy.getTop() : hierarchy.getBottom();
        }

        @Override
        public boolean equals(Object o) {
            return o == this;
        }

        @Override
        public int hashCode() {
            return 0;
        }

        @Override
        public String toString() {
            return isTop ? "<TOP>" : "<BOTTOM>";
        }
    }
}

