import org.checkerframework.common.value.qual.*;

// Test case for switch statements. Not really about the value checker (more about
// whether the semantics of switch are correct in general), but I needed some
// checker to try it out on.
class Switch {
    void test1(@IntVal({1, 2, 3, 4, 5}) int x) {

        // easy version, no fall through
        switch (x) {
            case 1:
                @IntVal({1}) int y = x;
                break;
            case 2:
                @IntVal({2}) int w = x;
                //:: error: (assignment.type.incompatible)
                @IntVal({1}) int z = x;
                break;
            default:
                // This should be a legal assignment, but dataflow is failing to
                // identify this as an else branch.
                //:: error: (assignment.type.incompatible)
                @IntVal({3, 4, 5}) int q = x;
                break;
        }
    }

    void test2(@IntVal({1, 2, 3, 4, 5}) int x) {

        // harder version, fall through
        switch (x) {
            case 1:
                @IntVal({1}) int y = x;
            case 2:
            case 3:
                @IntVal({1, 2, 3}) int w = x;
                //:: error: (assignment.type.incompatible)
                @IntVal({2, 3}) int z = x;
                //:: error: (assignment.type.incompatible)
                @IntVal({3}) int z1 = x;
                break;
            default:
                // This should be a legal assignment, but dataflow is failing to
                // identify this as an else branch.  See Issue 1180
                // https://github.com/typetools/checker-framework/issues/1180
                //:: error: (assignment.type.incompatible)
                @IntVal({4, 5}) int q = x;
                break;
        }
    }

    void test3(@IntVal({1, 2, 3, 4, 5}) int x) {

        // harder version, fall through
        switch (x) {
            case 1:
                @IntVal({1}) int y = x;
            case 2:
            case 3:
                @IntVal({1, 2, 3}) int w = x;
                //:: error: (assignment.type.incompatible)
                @IntVal({2, 3}) int z = x;
                //:: error: (assignment.type.incompatible)
                @IntVal({3}) int z1 = x;
                break;
            case 4:
            default:
                // This should be a legal assignment, but dataflow is failing to
                // identify this as an else branch.
                // https://github.com/typetools/checker-framework/issues/1180
                //:: error: (assignment.type.incompatible)
                @IntVal({4, 5}) int q = x;

                //:: error: (assignment.type.incompatible)
                @IntVal(5) int q2 = x;
                break;
        }
    }

    void test4(int x) {
        switch (x) {
            case 1:
                @IntVal({1}) int y = x;
                break;
            case 2:
            case 3:
                @IntVal({2, 3}) int z = x;
                break;
            case 4:
            default:
                return;
        }
        @IntVal({1, 2, 3}) int y = x;
        //:: error: (assignment.type.incompatible)
        @IntVal(4) int y2 = x;
    }

    void testInts(@IntRange(from = 0, to = 100) int x) {
        switch (x) {
            case 0:
            case 1:
            case 2:
                @IntVal({0, 1, 2}) int z = x;
                return;
            default:
        }

        @IntRange(from = 1, to = 100) int z = x;
    }

    void testChars(char x) {
        switch (x) {
            case 'a':
            case 2:
                @IntVal({'a', 2}) int z = x;
                break;
            case 'b':
                @IntVal('b') int v = x;
                break;
            default:
                return;
        }
        @IntVal({'a', 2, 'b'}) int y = x;
    }

    void testStrings(String s) {
        switch (s) {
            case "Good":
                @StringVal("Good") String x = s;
            case "Bye":
                @StringVal({"Good", "Bye"}) String y = s;
                break;
            case "Hello":
                @StringVal("Hello") String z = s;
                break;
            default:
                return;
        }
        @StringVal({"Good", "Bye", "Hello"}) String q = s;
    }
}
