import org.checkerframework.common.aliasing.qual.*;

public class TypeRefinement {

    /**
     * Type refinement is treated in the usual way, except that at (pseudo-)assignments the RHS may
     * lose its type refinement, before the LHS is type-refined.
     *
     * <p>The RHS always loses its type refinement (it is widened to @MaybeAliased, and its declared
     * type must have been @MaybeAliased) except in the following cases:
     *
     * <ol>
     *   <li>The RHS is a fresh expression.
     *   <li>The LHS is a @NonLeaked formal parameter and the RHS is an argument in a method call or
     *       constructor invocation.
     *   <li>The LHS is a @LeakedToResult formal parameter, the RHS is an argument in a method call
     *       or constructor invocation, and the method's return value is discarded.
     *       <ol>
     */

    // Test cases for the Aliasing type refinement cases below.
    // One method for each exception case. The usual case is tested in every method too.
    // As annotated in stubfile.astub, String() has type @Unique @NonLeaked.

    void rule1() {
        String unique = new String();
        // unique is refined to @Unique here, according to the definition.
        isUnique(unique);

        String notUnique = unique; // unique loses its refinement.

        // :: error: (argument.type.incompatible)
        isUnique(unique);
        // :: error: (argument.type.incompatible)
        isUnique(notUnique);
    }

    void rule2() {
        String unique = new String();

        isUnique(unique);
        nonLeaked(unique);
        isUnique(unique);

        leaked(unique);
        // :: error: (argument.type.incompatible)
        isUnique(unique);
    }

    void rule3() {
        String unique = new String();
        isUnique(unique);
        leakedToResult(unique);
        isUnique(unique);

        String notUnique = leakedToResult(unique);
        // :: error: (argument.type.incompatible)
        isUnique(unique);
    }

    void nonLeaked(@NonLeaked String s) {}

    void leaked(String s) {}

    String leakedToResult(@LeakedToResult String s) {
        return s;
    }

    // @NonLeaked so it doesn't refine the type of the argument.
    void isUnique(@NonLeaked @Unique String s) {}
}
