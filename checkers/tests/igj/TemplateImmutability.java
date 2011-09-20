import checkers.igj.quals.*;

/**
 * This class is for testing Template immutability
 *
 * @author mahmood
 */
@I
class Test {
    @I Test field;
    @Assignable @I Test assignable;
    @I Test getField(@ReadOnly Test this) { return field; }

    @I("O") Test getField(@ReadOnly Test this, @I("O") Test o) {
        this.assignable = assignable;
        assignable = this.assignable;
        this.assignable = o.assignable;  // emit error
        assignable = o.assignable;  // emit error
        o.assignable = assignable;  // emit error
        o.assignable = o.assignable;
        @I("O") Test f1 = o.field;
        @I("O") Test f2 = this.field; // emit error
        @I("O") Test f3 = field;     // emit error
        f1 = o.field;
        f1 = this.field; // emit error
        f1 = field;  // emit error
        return o.field;
    }

    static @I("O") Test getSame(@I("O") Test o) { return o; }

    void testOneParam(
        @Mutable Test mutable,
        @Immutable Test immutable,
        @ReadOnly Test readOnly) {

        mutable = getSame(mutable());
        immutable = getSame(mutable());     // emit error
        readOnly = getSame(mutable());

        mutable = getSame(immutable());     // emit error
        immutable = getSame(immutable());
        readOnly = getSame(immutable());

        mutable = getSame(readOnly());    // emit error
        immutable = getSame(readOnly());  // emit error
        readOnly = getSame(readOnly());
    }

    static @I("0") Test get() { return null; }

    void testUnidentified() {
        @Mutable Test mutable = null;
        @Immutable Test immutable = null;
        @ReadOnly Test readOnly = null;

        mutable = get();
        immutable = get();
        readOnly = get();
    }

    static @I("1") Test getUpperBound(@I("1") Test o1, @I("1") Test o2) {
        return null;
    }

    void testUpperBoundWithConcrete(@Mutable Test mutableTemp) {
        @Mutable Test mutable = mutable();
        @Immutable Test immutable = immutable();
        @ReadOnly Test readOnly = readOnly();

        {
            mutableTemp = getUpperBound(mutable, mutable);
            mutableTemp = getUpperBound(mutable, immutable);   // invalid
            mutableTemp = getUpperBound(mutable, readOnly);    // invalid
            mutableTemp = getUpperBound(immutable, mutable);   // invalid
            mutableTemp = getUpperBound(immutable, immutable); // invalid
            mutableTemp = getUpperBound(immutable, readOnly);  // invalid
            mutableTemp = getUpperBound(readOnly, mutable);    // invalid
            mutableTemp = getUpperBound(readOnly, immutable);  // invalid
            mutableTemp = getUpperBound(readOnly, readOnly);   // invalid
        }

        {
            @Immutable Test t1 = getUpperBound(mutable, mutable);   // invalid
            @Immutable Test t2 = getUpperBound(mutable, immutable); // invalid
            @Immutable Test t3 = getUpperBound(mutable, readOnly);  // invalid
            @Immutable Test t4 = getUpperBound(immutable, mutable); // invalid
            @Immutable Test t5 = getUpperBound(immutable, immutable);
            @Immutable Test t6 = getUpperBound(immutable, readOnly);// invalid
            @Immutable Test t7 = getUpperBound(readOnly, mutable);  // invalid
            @Immutable Test t8 = getUpperBound(readOnly, immutable);// invalid
            @Immutable Test t9 = getUpperBound(readOnly, readOnly); // invalid
        }

        {
            @ReadOnly Test t1 = getUpperBound(mutable, mutable);
            @ReadOnly Test t2 = getUpperBound(mutable, immutable);
            @ReadOnly Test t3 = getUpperBound(mutable, readOnly);
            @ReadOnly Test t4 = getUpperBound(immutable, mutable);
            @ReadOnly Test t5 = getUpperBound(immutable, immutable);
            @ReadOnly Test t6 = getUpperBound(immutable, readOnly);
            @ReadOnly Test t7 = getUpperBound(readOnly, mutable);
            @ReadOnly Test t8 = getUpperBound(readOnly, immutable);
            @ReadOnly Test t9 = getUpperBound(readOnly, readOnly);
        }
    }

    void testUpperBoundWithImmutabilityVar(@I("1") Test tm1, @I("2") Test tm2, @Mutable Test mutableTemp) {
//        @I("1") Test tm1 = null;
//        @I("2") Test tm2 = null;

        {
            @I("1") Test t1 = getUpperBound(tm1, tm1);
            @I("2") Test t2 = getUpperBound(tm1, tm1);  // invalid
            mutableTemp = getUpperBound(tm1, tm1); // invalid
            mutableTemp = getUpperBound(tm1, tm1);   // invalid
            @ReadOnly Test t5 = getUpperBound(tm1, tm1);
        }

        {
            @I("1") Test t1 = getUpperBound(tm1, tm2);  // invalid
            @I("2") Test t2 = getUpperBound(tm1, tm2);  // invalid
            mutableTemp = getUpperBound(tm1, tm2); // invalid
            @Immutable Test t4 = getUpperBound(tm1, tm2);   // invalid
            @ReadOnly Test t5 = getUpperBound(tm1, tm2);
        }

        {
            @I("1") Test t1 = getUpperBound(tm2, tm2);  // invalid
            @I("2") Test t2 = getUpperBound(tm2, tm2);
            mutableTemp = getUpperBound(tm2, tm2); // invalid
            @Immutable Test t4 = getUpperBound(tm2, tm2);   // invalid
            @ReadOnly Test t5 = getUpperBound(tm2, tm2);
        }

    }

    @Immutable Test immutable() { return null; }
    @Mutable Test mutable() { return null; }
    @ReadOnly Test readOnly() { return null; }
}
