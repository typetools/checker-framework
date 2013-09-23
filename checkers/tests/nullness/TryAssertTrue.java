// @skip-test crashes, but commented out to avoid breaking the build

public class TryAssertTrue {

    static int foo() { return 0; }

    static {
        long vmStartTime = 0;
        try {
            vmStartTime = foo();
        } catch (Throwable e) {
            assert true;
        }
    }

}
