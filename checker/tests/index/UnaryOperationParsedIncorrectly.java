import org.checkerframework.checker.index.qual.LessThan;

class Issue {
    void method1(@LessThan("#2") int var1, int var2) {
        // Function implementation
    }

    void method2() {
        method1(-10, 10); // This works fine
        method1(-10, +10); // This gives error
    }
}
