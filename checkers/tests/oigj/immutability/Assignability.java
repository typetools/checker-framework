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

    void readOnly(@ReadOnly Assignability this) {
        this.assignable = null;
        //:: error: (assignability.invalid)
        this.nonAssignable = null;

        readOnly.assignable = null;
        //:: error: (assignability.invalid)
        readOnly.nonAssignable = null;

        mutable.assignable = null;
        mutable.nonAssignable = null;

        immutable.assignable = null;
        //:: error: (assignability.invalid)
        immutable.nonAssignable = null;
    }

    void mutable(@Mutable Assignability this) {
        this.assignable = null;
        this.nonAssignable = null;

        readOnly.assignable = null;
        //:: error: (assignability.invalid)
        readOnly.nonAssignable = null;

        mutable.assignable = null;
        mutable.nonAssignable = null;

        immutable.assignable = null;
        //:: error: (assignability.invalid)
        immutable.nonAssignable = null;
    }

    void assignsFields(@AssignsFields Assignability this) {
        this.assignable = null;
        this.nonAssignable = null;

        readOnly.assignable = null;
        //:: error: (assignability.invalid)
        readOnly.nonAssignable = null;

        mutable.assignable = null;
        mutable.nonAssignable = null;

        immutable.assignable = null;
        //:: error: (assignability.invalid)
        immutable.nonAssignable = null;
    }

    void immutable(@Immutable Assignability this) {
        this.assignable = null;
        //:: error: (assignability.invalid)
        this.nonAssignable = null;

        readOnly.assignable = null;
        //:: error: (assignability.invalid)
        readOnly.nonAssignable = null;

        mutable.assignable = null;
        mutable.nonAssignable = null;

        immutable.assignable = null;
        //:: error: (assignability.invalid)
        immutable.nonAssignable = null;
    }

}
