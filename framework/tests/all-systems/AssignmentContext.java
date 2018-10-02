import org.checkerframework.common.value.qual.MinLen;

@SuppressWarnings("nullness") // Don't want to depend on @Nullable
class AssignmentContext {

    void foo(String[] a) {}

    @SuppressWarnings("determinism:invalid.type.on.conditional")
    void t1(boolean b) {
        String[] s = b ? new String[] {""} : null;
    }

    @SuppressWarnings("determinism")
    void t2(boolean b) {
        foo(b ? new String[] {""} : null);
    }

    @SuppressWarnings("determinism")
    String[] t3(boolean b) {
        return b ? new String[] {""} : null;
    }

    @SuppressWarnings("determinism:invalid.type.on.conditional")
    void t4(boolean b) {
        String[] s = null;
        s = b ? new String[] {""} : null;
    }

    @SuppressWarnings("determinism")
    void assignToCast(String @MinLen(4) [] @MinLen(5) [] currentSample) {
        // This statement used to cause a null pointer exception.
        ((String @MinLen(5) []) currentSample[3])[4] = currentSample[3][4];
    }
}
