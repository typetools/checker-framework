import java.io.*;

class TryWithResources {
    void m1(InputStream stream) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(stream))) {
            in.toString();
        } catch (Exception e) {
        }
    }

    void m2() {
        try (BufferedReader in = null) {
            // :: error: (dereference.of.nullable)
            in.toString();
        } catch (Exception e) {
        }
    }
}
