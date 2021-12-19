// @below-java17-jdk-skip-test
import org.checkerframework.common.value.qual.IntVal;

public class SwitchExpressionTyping {
    public static boolean flag = false;

    void method0(String s) {
        @IntVal({0, 1, 2, 3}) int o =
                switch (s) {
                    case "Hello?" -> {
                        throw new RuntimeException();
                    }
                    case "Hello" -> 0;
                    case "Bye" -> 1;
                    case "Later" -> 2;
                    case "What?" -> throw new RuntimeException();
                    default -> 3;
                };
    }

    void method1(String s) {
        @IntVal({1, 2, 3}) int o =
                switch (s) {
                    case "Hello?" -> 1;
                    case "Hello" -> 1;
                    case "Bye" -> 1;
                    case "Later" -> 1;
                    case "What?" -> {
                        if (flag) {
                            yield 2;
                        }
                        yield 3;
                    }
                    default -> 1;
                };

        @IntVal(1) int o2 =
                // :: error: (assignment)
                switch (s) {
                    case "Hello?" -> 1;
                    case "Hello" -> 1;
                    case "Bye" -> 1;
                    case "Later" -> 1;
                    case "What?" -> {
                        if (flag) {
                            yield 2;
                        }
                        yield 3;
                    }
                    default -> 1;
                };
    }

    void method2(String s, String r) {
        @IntVal({0, 1, 2, 3}) int o =
                switch (s) {
                    case "Hello?" -> {
                        if (flag) {
                            throw new RuntimeException();
                        }
                        yield 2;
                    }
                    case "Hello" -> {
                        int i =
                                switch (r) {
                                    case "Hello" -> 4;
                                    case "Bye" -> 5;
                                    case "Later" -> 6;
                                    default -> 42;
                                };
                        yield 0;
                    }
                    case "Bye" -> 1;
                    case "Later" -> {
                        int i =
                                switch (r) {
                                    case "Hello":
                                        {
                                            yield 4;
                                        }
                                    case "Bye":
                                        {
                                            yield 5;
                                        }
                                    case "Later":
                                        {
                                            yield 6;
                                        }
                                    default:
                                        {
                                            yield 42;
                                        }
                                };
                        yield 2;
                    }
                    case "What?" -> throw new RuntimeException();
                    default -> 3;
                };
    }
}
