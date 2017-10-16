import org.checkerframework.checker.index.qual.IndexFor;

// This is an explicit test that chains of field accesses and method calls are
// correctly invalidated.
public class ReassignmentChain {

    private ReassignmentChain parent;

    ReassignmentChain getParent() {
        return parent;
    }

    // Should not issue a warning.
    void test1(@IndexFor("parent.parent") int x) {}

    // Should issue a warning.
    void test2(@IndexFor("parent.parent") int x) {
        //:: error: (reassignment.field.not.permitted)
        parent = null;
    }
}
