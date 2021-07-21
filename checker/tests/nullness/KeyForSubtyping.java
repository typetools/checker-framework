import org.checkerframework.checker.nullness.qual.*;

import java.util.HashMap;

public class KeyForSubtyping {
    HashMap<String, String> mapA = new HashMap<>();
    HashMap<String, String> mapB = new HashMap<>();
    HashMap<String, String> mapC = new HashMap<>();

    public void testSubtypeAssignments(
            String not_a_key,
            @KeyFor("this.mapA") String a,
            @KeyFor("this.mapB") String b,
            @KeyFor({"this.mapA", "this.mapB"}) String ab) {
        // Try the error cases first, otherwise dataflow will change the inferred annotations on the
        // variables such that a line of code can have an effect on a subsequent line of code. We
        // want each of these tests to be independent.

        // :: error: (assignment.type.incompatible)
        ab = a;
        // :: error: (assignment.type.incompatible)
        ab = b;
        // :: error: (assignment.type.incompatible)
        a = b;
        // :: error: (assignment.type.incompatible)
        a = not_a_key;
        // :: error: (assignment.type.incompatible)
        b = not_a_key;
        // :: error: (assignment.type.incompatible)
        ab = not_a_key;

        // Now try the success cases

        a = ab;
        b = ab;
        not_a_key = ab;
        not_a_key = a;
    }

    public void testDataFlow(
            String not_yet_a_key,
            @KeyFor("this.mapA") String a,
            @KeyFor("this.mapB") String b,
            @KeyFor({"this.mapA", "this.mapB"}) String ab) {
        // Test that when a valid assignment is made, dataflow transfers the
        // KeyFor type qualifier from the right hand side to the left hand side.

        // :: error: (argument.type.incompatible)
        method1(not_yet_a_key);
        not_yet_a_key = a;
        method1(not_yet_a_key);

        method1(a);
        // :: error: (argument.type.incompatible)
        method1(b);
        method1(ab);

        b = ab;
        method1(b);
    }

    public void testSetOrdering(
            @KeyFor({"this.mapC", "this.mapA"}) String ac,
            @KeyFor({"this.mapA", "this.mapB", "this.mapC"}) String abc) {
        // Test that the order of elements in the set doesn't matter when doing subtyping checks,
        // @KeyFor("A, B, C") <: @KeyFor("C, A")

        // Try the error case first - see the note in method testSubtypeAssignments

        // :: error: (assignment.type.incompatible)
        abc = ac;

        ac = abc;
    }

    public void testDataflowTransitivity(
            @KeyFor({"this.mapA"}) String a,
            @KeyFor({"this.mapA", "this.mapB"}) String ab,
            @KeyFor({"this.mapA", "this.mapB", "this.mapC"}) String abc) {
        ab = abc;
        // At this point, dataflow should have refined the type of ab to
        // @KeyFor({"this.mapA","this.mapB","this.mapC"})
        a = ab;
        // At this point, dataflow should have refined the type of a to
        // @KeyFor({"this.mapA","this.mapB","this.mapC"})

        // This would not succeed without the previous two assignments, but should now because of
        // dataflow.
        abc = a;
    }

    private void method1(@KeyFor("this.mapA") String a) {}

    private void testWithNullnessAnnotation(
            String not_a_key,
            @KeyFor("this.mapA") String a,
            @KeyFor("this.mapB") String b,
            @Nullable @KeyFor({"this.mapA", "this.mapB"}) String ab) {
        // These fail only because a @Nullable RHS cannot be assigned to a @NonNull LHS.

        // :: error: (assignment.type.incompatible)
        a = ab;
        // :: error: (assignment.type.incompatible)
        b = ab;
        // :: error: (assignment.type.incompatible)
        not_a_key = ab;

        not_a_key = a; // Succeeds because both sides are @NonNull
    }

    // Test overriding

    static class Super {
        HashMap<String, String> map1 = new HashMap<>();
        HashMap<String, String> map2 = new HashMap<>();

        void method1(@KeyFor({"this.map1", "this.map2"}) String s) {}

        void method2(@KeyFor("this.map1") String s) {}
    }

    static class Sub extends Super {
        @Override
        void method1(@KeyFor("this.map1") String s) {}

        @Override
        // :: error: (override.param.invalid)
        void method2(@KeyFor({"this.map1", "this.map2"}) String s) {}
    }
}
