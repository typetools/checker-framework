class Issue450 {

    Issue450(int i, Runnable... runnables) {}

    Issue450(Consumer<String> consumer) {
        consumer.consume("hello"); // Use lambda as a constructor argument
    }

    interface Top {
        public void consume(String s);
    }

    interface Sub extends Top {
        public default void otherMethod() {}
    }

    interface Consumer<T> {
        void consume(T t);
    }

    void varargs(Runnable... runnables) {}

    public static void consumeStr(String str) {}

    public static void consumeStr2(String str) {}

    <E extends Consumer<String>> void context(E e, Sub s) {
        new Issue450(Issue450::consumeStr);

        Consumer<String> cs1 = (false) ? Issue450::consumeStr2 : Issue450::consumeStr;
        Consumer<String> cs2 = (false) ? e : Issue450::consumeStr;
        Top t = (false) ? s : Issue450::consumeStr;

        new Issue450(42, new Thread()::start); // Use lambda as a constructor argument
        varargs(new Thread()::start, new Thread()::start); // Use lambda in a var arg list of method
    }
}
