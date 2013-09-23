// @skip-test crashes, but commented out to avoid breaking the build

public class TryAssertTrue {

    static {
        long x = 0;
        try {
            x = 0;
        } catch (Throwable e) {
            assert true;
        }
    }

}
