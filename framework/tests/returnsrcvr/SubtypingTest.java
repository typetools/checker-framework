import org.checkerframework.common.returnsrcvr.qual.*;

// Test basic subtyping relationships for the Returns Receiver Checker.
class SubtypeTest {
    void allSubtypingRelationships(@MaybeThis int x, @BottomThis int y) {
        @MaybeThis int a = x;
        @MaybeThis int b = y;
        // :: error: assignment.type.incompatible
        @BottomThis int c = x; // expected error on this line
        @BottomThis int d = y;
    }
}
