
import org.checkerframework.checker.nullness.qual.*;

// Test shadowing of parameters

interface Consumer {
    void take(@Nullable String s);
}

interface NNConsumer {
    void take(String s);
}

class Shadowed {

    Consumer c = s -> {
        //:: error: (dereference.of.nullable)
        s.toString();

        class Inner {
            NNConsumer n = s -> {
                // No error
                s.toString();
            };
        }
    };
}
