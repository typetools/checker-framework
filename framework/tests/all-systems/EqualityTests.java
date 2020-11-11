public class EqualityTests {
    // the Interning checker correctly issues an error below, but we would like to keep this test in
    // all-systems.
    @SuppressWarnings("interning")
    public boolean compareLongs(Long v1, Long v2) {
        // This expression used to cause an assertion
        // failure in GLB computation.
        return !(((v1 == 0) || (v2 == 0)) && (v1 != v2));
    }

    public int charEquals(boolean cond) {
        char result = 'F';
        if (cond) {
            result = 'T';
        }

        if (result == 'T') {
            return 1;
        } else {
            assert result == '?';
        }
        return 10;
    }
}
