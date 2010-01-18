
public class AssertTwice {

    private void assertOnce() {
        String methodDeclaration = null;
        assert methodDeclaration != null : "@SuppressWarnings(nullness)";
        methodDeclaration = null;
    }

    private void assertTwice() {
        String methodDeclaration = null;
        assert methodDeclaration != null : "@SuppressWarnings(nullness)";
        assert methodDeclaration != null : "@SuppressWarnings(nullness)";
        methodDeclaration = null;
    }

    private void assertTwiceWithUse() {
        String methodDeclaration = null;
        assert methodDeclaration != null : "@SuppressWarnings(nullness)";
        methodDeclaration.toString();
        assert methodDeclaration != null : "@SuppressWarnings(nullness)";
        methodDeclaration = null;
    }

}
