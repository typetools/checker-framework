import org.checkerframework.checker.nullness.qual.*;

// Test shadowing of parameters

interface ConsumerS {
    void take(@Nullable String s);
}

interface NNConsumerS {
    void take(String s);
}

class Shadowed {

    ConsumerS c =
            s -> {
                // :: error: (dereference.of.nullable)
                s.toString();

                class Inner {
                    NNConsumerS n =
                            s -> {
                                // No error
                                s.toString();
                            };
                }
            };
}
