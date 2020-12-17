public class Catch {
    void defaultUnionType() throws Throwable {
        try {
            throw new Throwable();
        } catch (IndexOutOfBoundsException | NullPointerException ex) {

        }
    }

    void defaultDeclaredType() throws Throwable {
        try {
            throw new Throwable();
        } catch (RuntimeException ex) {

        }
    }
}
