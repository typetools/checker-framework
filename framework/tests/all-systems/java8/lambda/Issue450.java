import java.lang.CharSequence;

class Issue450 {

    interface Top {
        public void consume(String s);
    }

    interface Sub extends Top {
        default public void otherMethod() {
        }
    }

    interface Consumer<T> {
        void consume(T t);
    }

    Issue450(Consumer<String> consumer) {
        consumer.consume("hello");       // Use lambda as a constructor argument
    }

    public static void consumeStr(String str) {}
    public static void consumeStr2(String str) {}

    <E extends Consumer<String>> void context(E e, Sub s) {
        new Issue450(Issue450::consumeStr);

        Consumer<String> cs1 = (false) ? Issue450::consumeStr2 : Issue450::consumeStr;
        Consumer<String> cs2 = (false) ? e : Issue450::consumeStr;
        Top t = (false) ? s : Issue450::consumeStr;
    }
}