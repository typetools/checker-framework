public class PrimitiveDotClass {

    void test() {
        doStuff(int.class);
        doStuff(int[].class);
    }

    void doStuff(Class<?> cl) {}
}
