package determinism;

public class TryCatchException {
    void foo() {
        try {

        } catch (IndexOutOfBoundsException | NullPointerException ex) {
            // ::error: (throw.type.invalid)
            throw ex;
        } catch (IllegalArgumentException e) {
            // ::error: (throw.type.invalid)
            throw e;
        }
    }

    void bar() {
        try {
            throw new Throwable();
        } catch (RuntimeException ex) {
            // ::error: (argument.type.incompatible)
            System.out.println(ex);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
