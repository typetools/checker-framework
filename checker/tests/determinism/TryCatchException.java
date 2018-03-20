package determinism;

public class TryCatchException {
    void foo() {
        try {

        } catch (IndexOutOfBoundsException | NullPointerException ex) {
            // :: error: (throw.type.invalid)
            throw ex;
        } catch (IllegalArgumentException e) {
            throw e;
        }
    }
}
