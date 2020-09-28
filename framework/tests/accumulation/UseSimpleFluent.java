import org.checkerframework.framework.testchecker.testaccumulation.qual.*;

class UseSimpleFluent {
    static void req(@TestAccumulation({"a", "b"}) SimpleFluent s) {}

    static void test() {
        req(new SimpleFluent().a().b());
    }
}
