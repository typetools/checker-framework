import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;

class Basic {

    void required(@CompilerMessageKey String in) {}

    void test() {
        required("test.property");
    }
}
