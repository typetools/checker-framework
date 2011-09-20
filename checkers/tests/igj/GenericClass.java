import checkers.igj.quals.*;
import java.util.*;
/**
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
@I
public class GenericClass {
    int field = 5;
    @Assignable int assignable = 4;
    @I GenericClass next = null;

    // Fields
    @Mutable GenericClass mutableRef = new @Mutable GenericClass();
    @ReadOnly GenericClass readOnlyRef = new @ReadOnly GenericClass();

    public void mutableReassign(@Mutable GenericClass this) {
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

    public void roReassign(@ReadOnly GenericClass this) {
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

    public void roMethodInvoke(@ReadOnly GenericClass this) {
        mutableReassign();  // should emit error
        this.mutableReassign(); // should emit error

        this.roReassign();
        roReassign();
    }

    public void mutableRecieverInvoke(@Mutable GenericClass this) {
        mutableReassign();
        this.mutableReassign();

        this.roReassign();
        roReassign();
    }

    public void mutateOther(@ReadOnly GenericClass this) {
        @Mutable GenericClass instance = new @Mutable GenericClass();
        instance.mutableRecieverInvoke();
        instance.mutableReassign();
        instance.field = 5;
        instance.assignable = 3;
        instance.readOnlyRef.mutableReassign(); // should emit error
        instance.readOnlyRef.assignable = 2;
    }

    public void mutateOtherRO(@Mutable GenericClass this) {
        @ReadOnly GenericClass instance = new @ReadOnly GenericClass();
        instance.mutableRecieverInvoke(); //should emit error
        instance.mutableReassign(); //should emit error
        instance.field = 5; //should emit error
        instance.assignable = 3;
        instance.readOnlyRef.mutableReassign(); // should emit error
        instance.mutableRef.field++;
        instance.readOnlyRef.field++;   // should emit error
    }

    public void testConstructor(@ReadOnly GenericClass this) {
        @Mutable GenericClass c1 = new @Mutable GenericClass();
        @Immutable GenericClass c2 = new @Immutable GenericClass();

        @ReadOnly GenericClass c3 = new @ReadOnly GenericClass();
        @ReadOnly GenericClass c4 = new @Mutable GenericClass();
        @ReadOnly GenericClass c5 = new @Immutable GenericClass();

        @Mutable GenericClass c6 = new @Immutable GenericClass(); // should emit error
        @Immutable GenericClass c7 = new @Mutable GenericClass(); // should emit error
    }

    void testNewArray() {
        Object a = new ArrayList<Integer>();
    }
}
