public class AnoymousAndInnerClass {
    class MyInnerClass {
        public MyInnerClass() {}

        public MyInnerClass(String s) {}

        public MyInnerClass(int... i) {}
    }

    static class MyClass {
        public MyClass() {}

        public MyClass(String s) {}

        public MyClass(int... i) {}
    }

    void test(AnoymousAndInnerClass outer, String tainted) {
        new MyClass() {};
        new MyClass(tainted) {};
        new MyClass(1, 2, 3) {};
        new MyClass(1) {};
        new MyInnerClass() {};
        new MyInnerClass(tainted) {};
        new MyInnerClass(1) {};
        new MyInnerClass(1, 2, 3) {};
        this.new MyInnerClass() {};
        this.new MyInnerClass(tainted) {};
        this.new MyInnerClass(1) {};
        this.new MyInnerClass(1, 2, 3) {};
        outer.new MyInnerClass() {};
        outer.new MyInnerClass(tainted) {};
        outer.new MyInnerClass(tainted) {};
        outer.new MyInnerClass(1) {};
        outer.new MyInnerClass(1, 2, 3) {};
    }
}
