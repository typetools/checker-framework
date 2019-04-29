// Test field initialization
// fields, initializers, static initializers, constructors.

import org.checkerframework.checker.nullness.qual.*;

interface FunctionInit<T extends @Nullable Object, R> {
    R apply(T t);
}

interface Consumer<T> {
    void consume(T t);
}

// For test purposes, f1 is never initialized
@SuppressWarnings("initialization.fields.uninitialized")
class LambdaInit {
    String f1;
    String f2 = "";
    @Nullable String f3 = "";

    String f1b;
    FunctionInit<String, String> ff0 =
            s -> {
                // :: error: (dereference.of.nullable)
                f1.toString();
                // :: error: (dereference.of.nullable)
                f1b.toString();
                f2.toString();
                // :: error: (dereference.of.nullable)
                f3.toString();
                return "";
            };
    // Test field value refinement after initializer. f1b should still be @Nullable in the lambda.
    Object o1 = f1b = "";

    String f4;

    {
        f3 = "";
        f4 = "";
        FunctionInit<String, String> ff0 =
                s -> {
                    // :: error: (dereference.of.nullable)
                    f1.toString();
                    f2.toString();
                    // :: error: (dereference.of.nullable)
                    f3.toString();
                    f4.toString();
                    return "";
                };
    }

    String f5;

    LambdaInit() {
        f5 = "";
        FunctionInit<String, String> ff0 =
                s -> {
                    // :: error: (dereference.of.nullable)
                    f1.toString();
                    f2.toString();
                    // :: error: (dereference.of.nullable)
                    f3.toString();
                    f5.toString();
                    return "";
                };
    }

    //    // This is a bug
    //    // Could probably be fixed with CommittmentTreeAnnotator::visitMethod
    //    // Or more likely, TypeFromTree::212
    //    // AnnotatedTypeFactory::getImplicitReceiverType::1146(there is a todo...)
    //    Object o = new Object() {
    //        @Override
    //        public String toString() {
    //            f1.toString();
    //            f2.toString();
    //            return "";
    //        }
    //    };
    //

    //  Works!
    void method() {
        FunctionInit<String, String> ff0 =
                s -> {
                    f1.toString();
                    f2.toString();
                    // :: error: (dereference.of.nullable)
                    f3.toString();
                    return "";
                };
    }

    // Test for nested
    class Nested {
        FunctionInit<String, String> ff0 =
                s -> {
                    f1.toString();
                    f2.toString();
                    // :: error: (dereference.of.nullable)
                    f3.toString();
                    return "";
                };

        String f4;

        {
            f3 = "";
            f4 = "";
            FunctionInit<String, String> ff0 =
                    s -> {
                        f1.toString();
                        f2.toString();
                        // :: error: (dereference.of.nullable)
                        f3.toString();
                        f4.toString();
                        return "";
                    };
        }

        String f5;

        Nested() {
            f5 = "";
            FunctionInit<String, String> ff0 =
                    s -> {
                        f1.toString();
                        f2.toString();
                        // :: error: (dereference.of.nullable)
                        f3.toString();
                        f5.toString();
                        return "";
                    };
        }

        void method() {
            FunctionInit<String, String> ff0 =
                    s -> {
                        f1.toString();
                        f2.toString();
                        // :: error: (dereference.of.nullable)
                        f3.toString();
                        return "";
                    };
        }
    }

    // Test for nested in a lambda
    Consumer<String> func =
            s -> {
                Consumer<String> ff0 =
                        s2 -> {
                            // :: error: (dereference.of.nullable)
                            f1.toString();
                            f2.toString();
                            // :: error: (dereference.of.nullable)
                            f3.toString();
                        };
            };

    // Tests for static initializers.
    static String sf1;
    static String sf2 = "";
    static @Nullable String sf3 = "";
    static String sf1b;
    static FunctionInit<String, String> sff0 =
            s -> {

                // This is an issue with static initializers in general
                // // :: error: (dereference.of.nullable)
                sf1.toString();
                // This is an issue with static initializers in general
                // // :: error: (dereference.of.nullable)
                sf1b.toString();
                sf2.toString();
                // :: error: (dereference.of.nullable)
                sf3.toString();
                return "";
            };
    // Test field value refinement after initializer. f1b should still be null.
    static Object so1 = sf1b = "";

    static String sf4;

    static {
        sf3 = "";
        sf4 = "";
        FunctionInit<String, String> sff0 =
                s -> {

                    // This is an issue with static initializers in general
                    // // :: error: (dereference.of.nullable)
                    sf1.toString();
                    sf2.toString();
                    // :: error: (dereference.of.nullable)
                    sf3.toString();
                    sf4.toString();
                    return "";
                };
    }
}
