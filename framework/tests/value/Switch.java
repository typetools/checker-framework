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
                @IntVal({4, 5}) int q = x;
                break;
        }
    }
}
