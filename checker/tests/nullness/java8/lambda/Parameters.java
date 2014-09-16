
// The location of a lambda affects locating the annotation for the lambda.

import org.checkerframework.checker.nullness.qual.*;

interface Consumer {
    void method(@Nullable String s);
}
interface NNConsumer {
    void method(@NonNull String s);
}

class LambdaParam {

    //:: error: (lambda.param.type.incompatible)
    Consumer fn1 = (@NonNull String i) -> {};
    Consumer fn2 = (@Nullable String i) -> {};
    Consumer fn3 = (String i) -> {};
    NNConsumer fn4 = (String i) -> {};
    NNConsumer fn5 = (@Nullable String i) -> {};
    NNConsumer fn6 = (@NonNull String i) -> {};

// Initializer blocks with annotations don't work yet because of javac compiler bug.
// https://bugs.openjdk.java.net/browse/JDK-8056970
//    {
//          //:: error: (lambda.param.type.incompatible)
//        Consumer fn1 = (@NonNull String i) -> {};
//        Consumer fn2 = (@Nullable String i) -> {};
//        Consumer fn3 = (String i) -> {};
//        NNConsumer fn4 = (String i) -> {};
//        NNConsumer fn5 = (@Nullable String i) -> {};
//        NNConsumer fn6 = (@NonNull String i) -> {};
//    }
//
//    static {
//          //:: error: (lambda.param.type.incompatible)
//        Consumer fn1 = (@NonNull String i) -> {};
//        Consumer fn2 = (@Nullable String i) -> {};
//        Consumer fn3 = (String i) -> {};
//        NNConsumer fn4 = (String i) -> {};
//        NNConsumer fn5 = (@Nullable String i) -> {};
//        NNConsumer fn6 = (@NonNull String i) -> {};
//    }

    static void foo() {
        //:: error: (lambda.param.type.incompatible)
        Consumer fn1 = (@NonNull String i) -> {};
        Consumer fn2 = (@Nullable String i) -> {};
        Consumer fn3 = (String i) -> {};
        NNConsumer fn4 = (String i) -> {};
        NNConsumer fn5 = (@Nullable String i) -> {};
        NNConsumer fn6 = (@NonNull String i) -> {};
    }

    void bar() {
        //:: error: (lambda.param.type.incompatible)
        Consumer fn1 = (@NonNull String i) -> {};
        Consumer fn2 = (@Nullable String i) -> {};
        Consumer fn3 = (String i) -> {};
        NNConsumer fn4 = (String i) -> {};
        NNConsumer fn5 = (@Nullable String i) -> {};
        NNConsumer fn6 = (@NonNull String i) -> {};
    }
}
