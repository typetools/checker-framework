// Test case for PR #674
// https://github.com/typetools/checker-framework/pull/674
// @skip-test
import polyall.quals.*;

class ReplacingAnnos {
    @H1S1 Object addH1S2 = null;

    void foo(@H1S2 Object o) {
        // PolyAllChecker changes the type of addH1S2 to @H1S2 so the method call below should
        // typecheck
        foo(addH1S2);
    }
}
