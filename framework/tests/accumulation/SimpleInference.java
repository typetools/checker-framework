import testaccumulation.qual.*;

class SimpleInference {
    void build(@TestAccumulation({"a"}) SimpleInference this) {}

    void a() {}

    static void doStuffCorrect() {
        SimpleInference s = new SimpleInference();
        s.a();
        s.build();
    }

    static void doStuffWrong() {
        SimpleInference s = new SimpleInference();
        // :: error: method.invocation.invalid
        s.build();
    }
}
