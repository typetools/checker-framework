package determinism;

public class TryCatchException {
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
