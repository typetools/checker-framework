import java.io.IOException;
import java.security.GeneralSecurityException;

abstract class Issue4381 {

    public void t() {
        int m = 0;
        try {
            f();
        } catch (IllegalArgumentException | IOException e) {
        } catch (GeneralSecurityException e) {
            g(m);
        }
    }

    abstract void g(int x);

    abstract void f() throws IllegalArgumentException, IOException, GeneralSecurityException;
}
