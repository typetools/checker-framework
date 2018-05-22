package determinism;

public class TryCatchError {
    void foo() {
        try {

        } catch (LinkageError e) {
            throw e;
        } catch (AssertionError e) {
            throw e;
        }
    }
}
