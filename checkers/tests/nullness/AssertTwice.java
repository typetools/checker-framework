
public class AssertTwice {

    private void assertOnce() {
        String methodDeclaration = null;
        assert methodDeclaration != null;
        methodDeclaration = null;
    }

    private void assertTwice() {
        String methodDeclaration = null;
        assert methodDeclaration != null;
        assert methodDeclaration != null;
        methodDeclaration = null;
    }

    private void assertTwiceWithUse() {
        String methodDeclaration = null;
        assert methodDeclaration != null : "@AssumeAssertion(nullness)";
        methodDeclaration.toString();
        //:: warning: (known.nonnull)
        assert methodDeclaration != null;
        methodDeclaration = null;
    }

    public static @checkers.nullness.quals.Nullable Object n = "m";
    private void twiceWithChecks() {
        assert n != null;
        n = null;
    }
}
