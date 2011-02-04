import checkers.oigj.quals.*;

/**
 * Tests the immutability-part requirement of field assignability
 *
 */
public class Assignability {
    @Assignable Object assignable;
    Object nonAssignable;

    static @ReadOnly Assignability readOnly;
    static @Mutable Assignability  mutable;
    static @Immutable Assignability immutable;

    void readOnly() @ReadOnly {
        this.assignable = null;
        //:: (assignability.invalid)
        this.nonAssignable = null;

        readOnly.assignable = null;
        //:: (assignability.invalid)
        readOnly.nonAssignable = null;

        mutable.assignable = null;
        mutable.nonAssignable = null;

        immutable.assignable = null;
        //:: (assignability.invalid)
        immutable.nonAssignable = null;
    }

    void mutable() @Mutable {
        this.assignable = null;
        this.nonAssignable = null;

        readOnly.assignable = null;
        //:: (assignability.invalid)
        readOnly.nonAssignable = null;

        mutable.assignable = null;
        mutable.nonAssignable = null;

        immutable.assignable = null;
        //:: (assignability.invalid)
        immutable.nonAssignable = null;
    }

    void assignsFields() @AssignsFields {
        this.assignable = null;
        this.nonAssignable = null;

        readOnly.assignable = null;
        //:: (assignability.invalid)
        readOnly.nonAssignable = null;

        mutable.assignable = null;
        mutable.nonAssignable = null;

        immutable.assignable = null;
        //:: (assignability.invalid)
        immutable.nonAssignable = null;
    }

    void immutable() @Immutable {
        this.assignable = null;
        //:: (assignability.invalid)
        this.nonAssignable = null;

        readOnly.assignable = null;
        //:: (assignability.invalid)
        readOnly.nonAssignable = null;

        mutable.assignable = null;
        mutable.nonAssignable = null;

        immutable.assignable = null;
        //:: (assignability.invalid)
        immutable.nonAssignable = null;
    }

}
