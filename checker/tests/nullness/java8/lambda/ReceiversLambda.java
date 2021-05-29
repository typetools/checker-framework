// Test references to this and super in a lambda.

import org.checkerframework.checker.nullness.qual.*;

// Tests for the nullable type system
interface SupplierR {
  @NonNull ReceiverTest supply();
}

interface FunctionRT<T extends @Nullable Object, R> {
  R apply(T t);
}

class ReceiverTest {

  // :: error: (method.invocation)
  FunctionRT<String, String> f1 = s -> this.toString();
  // :: error: (method.invocation)
  FunctionRT<String, String> f2 = s -> super.toString();

  // :: error: (nullness.on.receiver)
  void context1(@NonNull ReceiverTest this) {
    SupplierR s = () -> this;
  }

  // :: error: (nullness.on.receiver)
  void context2(@Nullable ReceiverTest this) {
    // TODO: This is bug that is not specific to lambdas
    // https://github.com/typetools/checker-framework/issues/352
    // :: error: (return)
    SupplierR s = () -> this;
  }
}
