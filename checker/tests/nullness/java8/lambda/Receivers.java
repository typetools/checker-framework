
// Test references to this and super in a lambda.

import org.checkerframework.checker.nullness.qual.*;

// Tests for the nullable type system
interface Supplier {
    @NonNull ReceiverTest supply();
}

interface Function<T extends @Nullable Object, R> {
    R apply(T t);
}

class ReceiverTest {

    //:: error: (method.invocation.invalid)
    Function<String, String> f1 = s -> this.toString();
    Function<String, String> f2 = s -> super.toString();

    void context1(@NonNull ReceiverTest this) {
        Supplier s = () -> this;
    }
    void context2(@Nullable ReceiverTest this) {
        // TODO: This is bug that is not specific to lambdas
        // https://code.google.com/p/checker-framework/issues/detail?id=352
        //:: error: (return.type.incompatible)
        Supplier s = () -> this;
    }
}

