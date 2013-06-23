import java.io.*;

class TryWithResources {
    void m1(InputStream stream) {
        try(BufferedReader in =
            new BufferedReader(new InputStreamReader(stream))) {
            // Whatever.
        } catch(Exception e) {
        }
    }

    void m2() {
        //:: error: (assignment.type.incompatible)
        try(BufferedReader in = null) {
            // Whatever.
        } catch(Exception e) {
        }
    }
}