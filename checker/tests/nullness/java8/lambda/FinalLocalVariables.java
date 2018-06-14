// Test enclosing final local variables

import org.checkerframework.checker.nullness.qual.*;

interface FunctionLE<T extends @Nullable Object, R> {
    R apply(T t);
}

class LambdaEnclosing {

    // Test static initializer
    static {
        String local1 = "";
        String local2 = null;
        FunctionLE<String, String> f0 =
                s -> {
                    local1.toString();
                    // :: error: (dereference.of.nullable)
                    local2.toString();
                    return "";
                };
    }

    // Test instance initializer
    {
        String local1 = "";
        String local2 = null;
        FunctionLE<String, String> f0 =
                s -> {
                    local1.toString();
                    // :: error: (dereference.of.nullable)
                    local2.toString();
                    return "";
                };
    }

    FunctionLE<String, String> functionField =
            s -> {
                String local1 = "";
                String local2 = null;
                FunctionLE<String, String> f0 =
                        s2 -> {
                            local1.toString();
                            // :: error: (dereference.of.nullable)
                            local2.toString();
                            return "";
                        };
                return "";
            };

    void context() {
        String local1 = "";
        String local2 = null;

        FunctionLE<String, String> f1 =
                s -> {
                    local1.toString();
                    // :: error: (dereference.of.nullable)
                    local2.toString();
                    class Inner {

                        void context2() {
                            String local3 = "";
                            String local4 = null;

                            FunctionLE<String, String> f2 =
                                    s2 -> {
                                        local1.toString();
                                        local2.toString();
                                        local3.toString();
                                        // :: error: (dereference.of.nullable)
                                        local4.toString();

                                        return "";
                                    };
                        }
                    }

                    new Object() {

                        @Override()
                        public String toString() {
                            String local3 = "";
                            String local4 = null;

                            FunctionLE<String, String> f2 =
                                    s2 -> {
                                        local1.toString();
                                        local2.toString();
                                        local3.toString();
                                        // :: error: (dereference.of.nullable)
                                        local4.toString();

                                        return "";
                                    };
                            return "";
                        }
                    }.toString();

                    return "";
                };
    }
}
