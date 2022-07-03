class Issue5189b {
    class Inner {
        Inner(String... args) {}
    }

    void foo() {
        Object o = new Inner() {};
    }
}
