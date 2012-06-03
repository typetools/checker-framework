import java.util.Date;

/**
 * This file contains some failing test cases.  Should be fixed relatively soon.
 */
public class FailedTests {

    // Type Variables upper bound
    // We decided to have the upperbound of a type variable or a wildcard be
    // readonly within a type or method declaration and mutable otherwise.
    // The decision is done due to practicality.  Making all bounds readonly,
    // would result in false positives when using raw types or types with ? as
    // type argument (e.g. List<?>).  Making all bounds mutable makes all
    // unannotated java code compatible with IGJ, but prevent us from having
    // List<@Immutable String>.
    //
    // The proposed distiction between declaration and uses reduce the number
    // of false positives and ease the burden of annotating libraries
    //
    // However, there is one limiting case, which is the following:
    class MyList<T extends Date> { // T is 'T extends @ReadOnly Date'
        T readonlyDate;
        void test() {
            readonlyDate.setMonth(2);
        }
    }
}
