public class TestStub {
    void demo() {
        try {
            // :: error: (methodcall)
            Class.forName("Evil");
        } catch (Exception e) {
        }
    }
}
