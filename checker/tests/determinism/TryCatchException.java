package determinism;

public class TryCatchException {
    void foo() {
        try {

        } catch (IndexOutOfBoundsException | NullPointerException ex) {
            throw ex;
        } catch (IllegalArgumentException e) {
            throw e;
        }
    }

    void bar() {
        try {
            throw new Throwable();
        } catch (RuntimeException ex) {
            System.out.println(ex);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
