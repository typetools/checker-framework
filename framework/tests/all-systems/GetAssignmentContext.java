public class GetAssignmentContext {

    public A get() {
        return new A();
    }

    public A get1(String a) {
        return new A();
    }

    public void test() {
        GetAssignmentContext t = new GetAssignmentContext();
        // get().a should be skipped
        t.get1(get().a);
        t.get1(get().bar());
    }

    static class A {

        String a = "1";

        String bar(A this) {
            return "";
        }
    }
}
