import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;

public class Basic {

    void required(@CompilerMessageKey String in) {}

    void test() {
        required("test.property");
    }
}
