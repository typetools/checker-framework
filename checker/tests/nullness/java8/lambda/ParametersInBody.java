

// Test that parameter annotations are correct in the body of a lambda

import org.checkerframework.checker.nullness.qual.*;

interface Consumer {
    void method(@Nullable String s);
}
interface NNConsumer {
    void method(@NonNull String s);
}

class LambdaParamBody {

    //:: error: (dereference.of.nullable)
    Consumer fn0 = (String i) -> i.toString();
    //:: error: (dereference.of.nullable)
    Consumer fn2 = (@Nullable String i) -> {i.toString();};
    //:: error: (dereference.of.nullable)
    Consumer fn3 = (String i) -> {i.toString();};
    //:: error: (dereference.of.nullable)
    Consumer fn3b = (i) -> {i.toString();};

    NNConsumer fn4 = (String i) -> {i.toString();};
    NNConsumer fn4b = (i) -> {i.toString();};
    //:: error: (dereference.of.nullable)
    NNConsumer fn5 = (@Nullable String i) -> {i.toString();};
    NNConsumer fn6 = (@NonNull String i) -> {i.toString();};
}
