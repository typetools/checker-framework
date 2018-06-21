package determinism;

public class TryCatchException {
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
