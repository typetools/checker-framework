package determinism;

public class TryCatchError {
    void foo() {
        try {

        } catch (LinkageError e) {
            // ::error: (throw.type.invalid)
            throw e;
        } catch (AssertionError e) {
            // ::error: (throw.type.invalid)
            throw e;
        }
    }
}
