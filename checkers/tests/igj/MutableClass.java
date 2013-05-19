import checkers.igj.quals.*;

/**
 *
 * This is a test class for the IGJ Checker framework
 *
 * This class tests the basic operations, such as:
 *
 * <ul>
 *  <li> Mutation through references
 *  <li> Subtyping: Assignments, method invocations
 *  <li> Fields Rule (without any recursion)
 *  <li> method invocation rule (partially)
 * </ul>
 *
 * This class does not test for {@code @AssignsFields} methods
 */
@Mutable
public class MutableClass {
    int field = 5;
    @Assignable int assignable = 4;

    // Fields
    @Mutable MutableClass mutableRef = new @Mutable MutableClass();
    @ReadOnly MutableClass readOnlyRef = new @ReadOnly MutableClass();

    public void mutableReassign(@Mutable MutableClass this) {
        // try to mutate
        field = 3;
        field += 2;
        field++;
        --field;
        this.field++;
        this.field = 3;
        this.field += 2;
        this.field++;

        // reassigning an Assignable Field
        assignable = 3;
        assignable += 2;
        assignable++;
        --assignable;
        this.assignable++;
        this.assignable = 3;
        this.assignable += 2;
        this.assignable++;

        mutableRef = null;
        readOnlyRef = null;
    }

    public void roReassign(@ReadOnly MutableClass this) {
        // try to mutate
        field = 3;  // should emit error
        field += 2; // should emit error
        field++;    // should emit error
        --field;    // should emit error
        this.field++;   // should emit error
        this.field = 3; // should emit error
        this.field += 2;    // should emit error
        this.field++;   // should emit error

        // reassigning an Assignable Field
        assignable = 3;
        assignable += 2;
        assignable++;
        --assignable;
        this.assignable++;
        this.assignable = 3;
        this.assignable += 2;
        this.assignable++;

        mutableRef = null;  // should emit error
        readOnlyRef = null; // should emit error
    }

    // Method Calling

    public void roMethodInvoke(@ReadOnly MutableClass this) {
        mutableReassign();  // should emit error
        this.mutableReassign(); // should emit error

        this.roReassign();
        roReassign();
    }

    public void mutableRecieverInvoke(@Mutable MutableClass this) {
        mutableReassign();
        this.mutableReassign();

        this.roReassign();
        roReassign();
    }

    public void mutateOther(@ReadOnly MutableClass this) {
        @Mutable MutableClass instance = new @Mutable MutableClass();
        instance.mutableRecieverInvoke();
        instance.mutableReassign();
        instance.field = 5;
        instance.assignable = 3;
        instance.readOnlyRef.mutableReassign(); // should emit error
        instance.readOnlyRef.assignable = 2;
    }

    public void mutateOetherRO(@Mutable MutableClass this) {
        @ReadOnly MutableClass instance = new @ReadOnly MutableClass();
        instance.mutableRecieverInvoke(); //should emit error
        instance.mutableReassign(); //should emit error
        instance.field = 5; //should emit error
        instance.assignable = 3;
        instance.readOnlyRef.mutableReassign(); // should emit error
        instance.mutableRef.field++;
        instance.readOnlyRef.field++;   // should emit error
    }
}
