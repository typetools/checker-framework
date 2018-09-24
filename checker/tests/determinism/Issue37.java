import org.checkerframework.checker.determinism.qual.*;

class Issue37 {
    void testLineSeparator() {
        System.out.println(System.getProperty("line.separator"));
    }

    void testPathSeparator() {
        System.out.println(System.getProperty("path.separator"));
    }
}
