
public class EqualityTests {
    public boolean compareLongs(Long v1, Long v2) {
        // This expression used to cause an assertion
        // failure in GLB computation.
        return !(((v1 == 0) || (v2 == 0)) && (v1 != v2));
    }
}


