class Issue450 {

    interface Consumer<T> {
        void consume(T t);
    }

    Issue450(Consumer<String> consumer) {
        consumer.consume("hello");       // Use lambda as a constructor argument
    }

    public static void consumeStr(String str) {}

    void context() {
        new Issue450(Issue450::consumeStr);
    }
}